package com.zerosumtech.wparad.stash;
import java.util.Map;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.bitbucket.repository.Repository;

public class PluginEnabledCondition implements Condition
{
  private final RepositoryInformationService repositoryInformationService;
  
  public PluginEnabledCondition(RepositoryInformationService repositoryInformationService) 
  {
  	this.repositoryInformationService = repositoryInformationService;
  }
  
  @Override
  public void init(Map<String, String> context) throws PluginParseException { }
  
  @Override
  public boolean shouldDisplay(Map<String, Object> context) 
  {
	  final Object obj = context.get("repository");
	  if (obj == null || !(obj instanceof Repository)) { return false; }

	  return repositoryInformationService.ArePullRequestsConfigured((Repository) obj);
  }
}
