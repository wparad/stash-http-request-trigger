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

public class RepositoryInformationService 
{
	private static final Logger logger = LoggerFactory.getLogger(RepositoryInformationService.class);
	
	private static final String PLUGIN_KEY = "com.zerosumtech.wparad.stash.stash-http-request-trigger:postReceiveHook";
	private final PermissionValidationService permissionValidationService;
	private final RepositoryHookService repositoryHookService;
	  
	public RepositoryInformationService(
			PermissionValidationService permissionValidationService, 
			RepositoryHookService repositoryHookService) 
	{
		this.permissionValidationService = permissionValidationService;
	  	this.repositoryHookService = repositoryHookService;
	}
	  
	public boolean IsPluginEnabled(Repository repository)
	{
		permissionValidationService.validateForRepository(repository, Permission.REPO_READ);
		RepositoryHook repositoryHook = repositoryHookService.getByKey(repository, PLUGIN_KEY);
		return repositoryHook != null && repositoryHook.isEnabled() && GetSettings(repository) != null;
	}

	private Settings GetSettings(Repository repository)
	{
		permissionValidationService.validateForRepository(repository, Permission.REPO_READ);
		return repositoryHookService.getSettings(repository, PLUGIN_KEY);
	}
  
	public boolean CheckFromRefChanged(Repository repository)
	{
		Settings settings = GetSettings(repository);
		return settings != null && settings.getBoolean("checkFromRefChanged", false);
	}

	public void PostChange(Repository repository, String ref, String sha, String pullRequestNbr)
	{
		if(!IsPluginEnabled(repository)) { return; }
		Post(GetUrl(repository, ref, sha, pullRequestNbr));
	}
  
	public String GetUrl(Repository repository, String ref, String sha, String pullRequestNbr)
	{
		String urlParams = null;
		try 
		{
			urlParams = "STASH_REF=" + URLEncoder.encode(ref, "UTF-8") + "&STASH_SHA=" + URLEncoder.encode(sha, "UTF-8");
			if(pullRequestNbr != null){ urlParams += "&STASH_PULL_REQUEST=" + pullRequestNbr;}
		} 
		catch (UnsupportedEncodingException e) 
		{
			logger.error("Failed to get URL ({}, {}, {}, {})", new Object[]{repository.getName(), ref, sha, pullRequestNbr});
			throw new RuntimeException(e);
		}

		//If the URL already includes query parameters then append them
		String baseUrl = GetSettings(repository).getString("url");
		int index = baseUrl.indexOf("?");
		return baseUrl.concat( (index == -1 ? "?" : "&") + urlParams);
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
