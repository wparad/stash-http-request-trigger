package com.zerosumtech.wparad.stash;

import java.io.DataOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import com.atlassian.stash.hook.repository.RepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.PermissionValidationService;

public class RepositoryInformationService 
{
	  private static final String PLUGIN_KEY = "com.zerosumtech.wparad.stash.stash-http-request-trigger";
	  private static final String HOOK_KEY = "postReceiveHook";
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
		  RepositoryHook repositoryHook = repositoryHookService.getByKey(repository, PLUGIN_KEY + ":" + HOOK_KEY);
		  Settings settings = repositoryHookService.getSettings(repository, PLUGIN_KEY + ":" + HOOK_KEY);
		  return repositoryHook != null && repositoryHook.isEnabled() && settings != null;
	  }

	  private Settings GetSettings(Repository repository)
	  {
		  permissionValidationService.validateForRepository(repository, Permission.REPO_READ);
		  return repositoryHookService.getSettings(repository, PLUGIN_KEY + ":" + HOOK_KEY);
	  }
	  
	  public boolean CheckFromRefChanged(Repository repository)
	  {
		  return GetSettings(repository).getBoolean("checkFromRefChanged", false);
	  }

	  public void PostChange(Repository repository, String ref, String sha, String pullRequestNbr)
	  {
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
			  e.printStackTrace();
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
		  } 
		  catch (Exception e) { e.printStackTrace(); }
	  }
}
