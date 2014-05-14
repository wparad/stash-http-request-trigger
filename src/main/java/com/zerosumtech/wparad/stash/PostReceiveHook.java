package com.zerosumtech.wparad.stash;

import com.atlassian.event.api.EventListener;
import com.atlassian.stash.event.pull.PullRequestOpenedEvent;
import com.atlassian.stash.event.pull.PullRequestReopenedEvent;
import com.atlassian.stash.event.pull.PullRequestRescopedEvent;
import com.atlassian.stash.event.RepositoryRefsChangedEvent;
import com.atlassian.stash.hook.repository.AsyncPostReceiveRepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.hook.repository.RepositoryHookService;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.RepositorySettingsValidator;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsValidationErrors;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.PermissionValidationService;

import java.io.DataOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostReceiveHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator 
{
	private static final String PLUGIN_KEY = "com.zerosumtech.wparad.stash.stash-http-request-trigger";
	private static final String HOOK_KEY = "postReceiveHook";
    private static final Logger log = LoggerFactory.getLogger(PostReceiveHook.class);
    private final PermissionValidationService permissionValidationService;
    private final RepositoryHookService repositoryHookService; 
    
    public PostReceiveHook(PermissionValidationService permissionValidationService, RepositoryHookService repositoryHookService) 
    {
    	this.permissionValidationService = permissionValidationService;
    	this.repositoryHookService = repositoryHookService;
    }

    @Override
    public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges) 
    {
		//Don't need to do this because the event triggers will handle the receive.
    }

    @EventListener
    public void onRefsChangedEvent(RepositoryRefsChangedEvent event) 
    {
    	Repository repository = event.getRepository();
    	for(RefChange refChange : event.getRefChanges())
    	{
    		if(refChange.getType() == RefChangeType.DELETE) {continue;}
    		postChange(repository, refChange.getRefId(), refChange.getToHash(), null);
    	}
    }

    @EventListener
    public void onPullRequestOpened(PullRequestOpenedEvent event) { HandlePullRequestEvent(event.getPullRequest()); }
    
    @EventListener
    public void onPullRequestReopened(PullRequestReopenedEvent event) { HandlePullRequestEvent(event.getPullRequest()); }

    @EventListener
    public void onPullRequestRescoped(PullRequestRescopedEvent event) 
	{
    	boolean checkFromRefChanged = GetSettings(event.getPullRequest().getToRef().getRepository()).getBoolean("checkFromRefChanged", false);
    	String previousHash = event.getPreviousFromHash();
    	String newHash = event.getPullRequest().getFromRef().getLatestChangeset();
		if(!checkFromRefChanged || previousHash == null || newHash == null || !previousHash.equals(newHash))
		{
			HandlePullRequestEvent(event.getPullRequest());
		}
	}

    private void HandlePullRequestEvent(PullRequest pullRequest)
    {
    	Repository repository = pullRequest.getToRef().getRepository();
    	String ref = "refs/pull-requests/" + Long.toString(pullRequest.getId());
		
		postChange(repository, ref, pullRequest.getFromRef().getLatestChangeset(), Long.toString(pullRequest.getId()));
    }
    private void postChange(Repository repository, String ref, String sha, String pullRequestNbr) 
    {
    	String baseUrl = GetSettings(repository).getString("url");
        String urlParams = null;
		try 
		{
			urlParams = "STASH_REF=" + URLEncoder.encode(ref, "UTF-8") + "&STASH_SHA=" + URLEncoder.encode(sha, "UTF-8");
			if(pullRequestNbr != null){ urlParams += "&STASH_PULL_REQUEST=" + pullRequestNbr;}
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
			log.error("Error in url-encoding query parameters.", e);
			throw new RuntimeException(e);
		}
		
        //If the URL already includes query parameters then append them
        int index = baseUrl.indexOf("?");
        if(index != -1)
        {
        	urlParams = urlParams.concat("&" + baseUrl.substring(index + 1));
        	baseUrl = baseUrl.substring(0, index);
        }
        post(baseUrl, urlParams);
    }

    private void post(String baseUrl, String urlParams) 
    {
        log.debug("post: " + baseUrl + "?" + urlParams);

        try 
        {
            URLConnection conn = new URL(baseUrl).openConnection();
            conn.setDoOutput(true);  // Triggers POST
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream ());
            wr.writeBytes(urlParams);
            wr.flush();
            wr.close();

            conn.getInputStream().close();
        } 
        catch (Exception e) { log.error("Error in post", e); }
    }

    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        String url = settings.getString("url");
        if (url == null || url.trim().isEmpty())  { errors.addFieldError("url", "URL field is blank, please supply one"); }
    }
    
    private Settings GetSettings(Repository repository)
    {
    	permissionValidationService.validateForRepository(repository, Permission.REPO_READ);
    	return repositoryHookService.getSettings(repository, PLUGIN_KEY + ":" + HOOK_KEY);
    }
}
