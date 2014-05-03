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
import com.atlassian.stash.pull.PullRequestRef;
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
	private static final String PLUGIN_KEY = "com.zerosumtech.wparad.stash-stash-http-request-trigger";
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
    		postChange(repository, refChange.getRefId(), refChange.getToHash());
    	}
    }

    @EventListener
    public void pullRequestOpened(PullRequestOpenedEvent event) { HandlePullRequestEvent(event.getPullRequest()); }
    
    @EventListener
    public void onPullRequestReopened(PullRequestReopenedEvent event) { HandlePullRequestEvent(event.getPullRequest()); }

    @EventListener
    public void pullRequestRescoped(PullRequestRescopedEvent event) { HandlePullRequestEvent(event.getPullRequest()); }

    private void HandlePullRequestEvent(PullRequest pullRequest)
    {
    	Repository repository = pullRequest.getToRef().getRepository();
    	PullRequestRef ref = pullRequest.getFromRef();
		postChange(repository, ref.getId(), ref.getLatestChangeset());
    }
    private void postChange(Repository repository, String ref, String sha) 
    {
    	permissionValidationService.validateForRepository(repository, Permission.REPO_READ);
    	Settings settings = repositoryHookService.getSettings(repository, PLUGIN_KEY + ":" + HOOK_KEY);
    	String baseUrl = settings.getString("url");
        String urlParams = "STASH_REF=" + urlEncode(ref) + "&STASH_SHA=" + urlEncode(sha);
        
        //If the url already includes query parameters then append them
        int index = baseUrl.indexOf("?");
        if(index != -1)
        {
        	urlParams.concat("&" + baseUrl.substring(index + 1));
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

    private static String urlEncode(String string) {
        try { return URLEncoder.encode(string, "UTF-8"); } 
        catch (UnsupportedEncodingException e) { throw new RuntimeException(e); }
    }

    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        String url = settings.getString("url");
        if (url == null || url.trim().isEmpty())  { errors.addFieldError("url", "URL field is blank, please supply one"); }
    }
}
