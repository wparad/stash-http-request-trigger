package com.zerosumtech.wparad.stash;

import java.util.Map;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.repository.Repository;
import com.google.common.collect.ImmutableMap;

public class RepositoryContextProvider implements ContextProvider
{
	private final RepositoryInformationService repositoryInformationService;

	public RepositoryContextProvider(RepositoryInformationService repositoryInformationService)
	{
		this.repositoryInformationService = repositoryInformationService;
	}

	@Override
	public void init(Map<String, String> params) throws PluginParseException {}

	@Override
	public Map<String, Object> getContextMap(Map<String, Object> context) 
	{
		Repository repository = (Repository)context.get("repository");
		PullRequest pullRequest = (PullRequest)context.get("pullRequest");
    	String ref = "refs/pull-requests/" + Long.toString(pullRequest.getId());
    	String buildUrl = repositoryInformationService.GetPullRequestUrl(repository,
    			ref,
    			pullRequest.getFromRef().getLatestChangeset(),
    			pullRequest.getToRef().getId(),
    			Long.toString(pullRequest.getId()),
    			true);
    	return ImmutableMap.<String, Object>builder().put("buildUrl", buildUrl).build();
	}
}