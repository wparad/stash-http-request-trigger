package com.zerosumtech.wparad.stash;

import java.io.DataOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.stash.hook.repository.RepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.PermissionValidationService;
import com.atlassian.stash.user.SecurityService;
import com.atlassian.stash.util.Operation;

public class RepositoryInformationService 
{
	private static final Logger logger = LoggerFactory.getLogger(RepositoryInformationService.class);
	
	private static final String PLUGIN_KEY = "com.zerosumtech.wparad.stash.stash-http-request-trigger:postReceiveHook";
	private final PermissionValidationService permissionValidationService;
	private final RepositoryHookService repositoryHookService;
	private final SecurityService securityService;
	  
	public RepositoryInformationService(
			PermissionValidationService permissionValidationService, 
			RepositoryHookService repositoryHookService,
			SecurityService securityService) 
	{
		this.permissionValidationService = permissionValidationService;
	  	this.repositoryHookService = repositoryHookService;
		this.securityService = securityService;
	}
	  
	public boolean IsPluginEnabled(final Repository repository)
	{
		permissionValidationService.validateForRepository(repository, Permission.REPO_READ);
		try 
		{
			return securityService.doWithPermission("Retrieving repository hook", Permission.REPO_ADMIN, new Operation<Boolean, Exception>()
			{
				@Override
				public Boolean perform() throws Exception 
				{
					RepositoryHook repositoryHook = repositoryHookService.getByKey(repository, PLUGIN_KEY); 
					return repositoryHook != null && repositoryHook.isEnabled() && repositoryHookService.getSettings(repository, PLUGIN_KEY) != null;
				}
			}).booleanValue();
		}
		catch (Exception e)
		{
			logger.error("Failed: IsPluginEnabled({})", repository.getName(), e);
			return false;
		}
	}

	private Settings GetSettings(final Repository repository)
	{
		permissionValidationService.validateForRepository(repository, Permission.REPO_READ);
		try 
		{
			return securityService.doWithPermission("Retrieving settings", Permission.REPO_ADMIN, new Operation<Settings, Exception>()
			{
				@Override
				public Settings perform() throws Exception { return repositoryHookService.getSettings(repository, PLUGIN_KEY); } 
			});
		}
		catch(Exception e)
		{
			logger.error("Failed: GetSettings({})", repository.getName(), e);
			return null;
		}
	}
  
	public boolean CheckFromRefChanged(final Repository repository)
	{
		Settings settings = GetSettings(repository);
		return settings != null && settings.getBoolean("checkFromRefChanged", false);
	}

	public void PostChange(Repository repository, String ref, String sha, String toRef, String pullRequestNbr)
	{
		if(!IsPluginEnabled(repository)) { return; }
		Post(GetUrl(repository, ref, sha, toRef, pullRequestNbr));
	}
  
	public String GetUrl(final Repository repository, String ref, String sha, String toRef, String pullRequestNbr)
	{
		StringBuilder urlParams = new StringBuilder();
		try 
		{
			urlParams.append("STASH_REF=" + URLEncoder.encode(ref, "UTF-8"));
			urlParams.append("&STASH_SHA=" + URLEncoder.encode(sha, "UTF-8"));
			if(pullRequestNbr != null)
			{
				urlParams.append("&STASH_TO_REF=" + URLEncoder.encode(toRef, "UTF-8"));
				urlParams.append("&STASH_PULL_REQUEST=" + pullRequestNbr);
			}
		} 
		catch (UnsupportedEncodingException e) 
		{
			logger.error("Failed to get URL ({}, {}, {}, {})", new Object[]{repository.getName(), ref, sha, pullRequestNbr});
			throw new RuntimeException(e);
		}

		//If the URL already includes query parameters then append them
		String baseUrl = GetSettings(repository).getString("url");
		int index = baseUrl.indexOf("?");
		return baseUrl.concat( (index == -1 ? "?" : "&") + urlParams.toString());
	}
  
	public void Post(String url) 
	{
		try 
		{
			logger.info("Begin Posting to URL: {}", url);
			int index = url.indexOf("?");
			String baseUrl = url.substring(0, index);
			String urlParams = url.substring(index + 1);
			URLConnection conn = new URL(baseUrl).openConnection();
			conn.setDoOutput(true);  // Triggers POST
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			DataOutputStream wr = new DataOutputStream(conn.getOutputStream ());
			wr.writeBytes(urlParams);
			wr.flush();
			wr.close();

			conn.getInputStream().close();
			logger.info("Success Posting to URL: {}", url);
		} 
		catch (Exception e)  { logger.error("Failed Posting to URL: {}", url, e); }
	}
}
