package com.zerosumtech.wparad.stash;
import java.util.Map;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.stash.hook.repository.RepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.PermissionValidationService;

public class PluginEnabledCondition implements Condition
{
  private static final String PLUGIN_KEY = "com.zerosumtech.wparad.stash.stash-http-request-trigger";
  private static final String HOOK_KEY = "postReceiveHook";
  private final PermissionValidationService permissionValidationService;
  private final RepositoryHookService repositoryHookService; 
  
  public PluginEnabledCondition(PermissionValidationService permissionValidationService, RepositoryHookService repositoryHookService) 
  {
  	this.permissionValidationService = permissionValidationService;
  	this.repositoryHookService = repositoryHookService;
  }
  
  @Override
  public void init(Map<String, String> context) throws PluginParseException { }
  
  @Override
  public boolean shouldDisplay(Map<String, Object> context) 
  {
	  final Object obj = context.get("repository");
	  if (obj == null || !(obj instanceof Repository)) { return false; }
    
	  Repository repository = (Repository) obj;
	  permissionValidationService.validateForRepository(repository, Permission.REPO_READ);
	  RepositoryHook repositoryHook = repositoryHookService.getByKey(repository, PLUGIN_KEY + ":" + HOOK_KEY);
	  Settings settings = repositoryHookService.getSettings(repository, PLUGIN_KEY + ":" + HOOK_KEY);
	  return repositoryHook != null && repositoryHook.isEnabled() && settings != null;
  }
}