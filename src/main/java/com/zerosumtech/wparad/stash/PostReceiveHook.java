package com.zerosumtech.wparad.stash;

import com.atlassian.stash.event.pull.PullRequestRescopedEvent;
import com.atlassian.stash.hook.repository.AsyncPostReceiveRepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.pull.PullRequestRef;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.RepositorySettingsValidator;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsValidationErrors;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collection;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventListener;
import com.atlassian.stash.event.RepositoryRefsChangedEvent;
import com.atlassian.stash.event.pull.PullRequestOpenedEvent;

public class PostReceieveHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator 
{

    private static final Logger log = LoggerFactory.getLogger(PostReceieveHook.class);
    private static final String REFS_HEADS = "refs/heads/";  //TODO: add in pull requests here, I don't even think this matters

    public PostReceieveHook(NavBuilder navBuilder) {
		String cloneUrl = navBuilder.repo(context.getRepository()).clone(context.getRepository().getScmId()).buildAbsolute();
		String branches = urlEncode(Joiner.on(",").join(b));
		//return context.getSettings().getString(CONFIG_URL, "").replace("$URL", urlEncode(cloneUrl)).replace("$BRANCHES", branches);
    }

    @Override
    public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges) 
    {
		/*
		 * Don't need to do this because it's in the repository change event
		 * // NOTE: Need to pass this down into postChange
        String url = context.getSettings().getString("url");

        Iterable<RefChange> updatedRefs = Iterables.filter(refChanges, new Predicate<RefChange>() 
        {
            @Override
            public boolean apply(RefChange input) {
                // We only care about non-deleted branches //TODO: probably care about more things than just heads here
                return input.getType() != RefChangeType.DELETE && input.getRefId().startsWith(REFS_HEADS);
            }
        });

        for (RefChange refChange : updatedRefs) 
        {
            log.info("Ref changed: " + refChange.getRefId());
            String ref = refChange.getRefId().replace(REFS_HEADS, "");

            String sha = refChange.getToHash();
            postChange(ref, sha);
        }
		 * */
    }

  /**
   * Event listener that is notified of both pull request merges and push events
   * @param event The pull request event
   */
    @EventListener
    public void onRefsChangedEvent(RepositoryRefsChangedEvent event) 
  {    
	String user = (event.getUser() != null) ? event.getUser().getName() : null;
	EventContext context = new EventContext(event, event.getRepository(), user);
	
	if (filterChain.shouldDeliverNotification(context))
	  postChange(fromRef.getId(), fromRef.getLatestChangeset());
	  
  }

    @EventListener
    public void pullRequestOpened(PullRequestOpenedEvent event) 
    {
        EventContext context = new EventContext(event, 
        event.getPullRequest().getToRef().getRepository(), 
        event.getUser().getName());
    
		if (filterChain.shouldDeliverNotification(context))
		  notifier.notifyBackground(context.getRepository());
		  
        PullRequestRef fromRef = event.getPullRequest().getFromRef();

        postChange(fromRef.getId(), fromRef.getLatestChangeset());
    }

    @EventListener
    public void pullRequestRescoped(PullRequestRescopedEvent event) 
    {
		EventContext context = new EventContext(event, 
        event.getPullRequest().getToRef().getRepository(), 
        event.getUser().getName());
    
		if (filterChain.shouldDeliverNotification(context))
		  notifier.notifyBackground(context.getRepository());
		  
        PullRequestRef fromRef = event.getPullRequest().getFromRef();

        if (!event.getPreviousFromHash().equals(fromRef.getLatestChangeset())) {
            postChange(fromRef.getId(), fromRef.getLatestChangeset());
        }
    }

    private void postChange(String ref, String sha) 
    {
        // Redefine this so it pulls from context
        String baseUrl = "http://localhost:3000/repositories/7/build-ref";
        String urlParams = "ref=" + urlEncode(ref) + "&sha=" + urlEncode(sha);

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
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        String url = settings.getString("url");
        if (url == null || url.trim().isEmpty()) {
            errors.addFieldError("url", "URL field is blank, please supply one");
        }
    }
}
