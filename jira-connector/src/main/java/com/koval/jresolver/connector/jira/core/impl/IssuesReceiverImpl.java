package com.koval.jresolver.connector.jira.core.impl;

import java.util.Collection;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.koval.jresolver.connector.jira.client.JiraClient;
import com.koval.jresolver.connector.jira.configuration.ConnectorProperties;
import com.koval.jresolver.connector.jira.core.IssuesReceiver;
import com.koval.jresolver.connector.jira.util.CollectionsUtil;


public class IssuesReceiverImpl implements IssuesReceiver {

  private static final Logger LOGGER = LoggerFactory.getLogger(IssuesReceiverImpl.class);

  private final ProgressMonitor progressMonitor;
  private final JiraClient client;
  private final String query;
  private final Set<String> fields;
  private final int batchSize;
  private int currentIndex;
  private final int finishIndex;
  private final int batchDelay;

  public IssuesReceiverImpl(JiraClient client, ConnectorProperties properties, boolean isResolvedMode) {
    this.client = client;
    this.query = isResolvedMode ? properties.getResolvedQuery() : properties.getUnresolvedQuery();
    this.fields = isResolvedMode ? properties.getResolvedIssueFields() : properties.getUnresolvedIssueFields();
    this.batchSize = properties.getBatchSize();
    this.batchDelay = properties.getBatchDelay();
    this.currentIndex = 0;
    this.finishIndex = getTotalIssues();
    this.progressMonitor = new ProgressMonitor(batchSize, finishIndex);
  }

  private int getTotalIssues() {
    LOGGER.info("Getting total issues...");
    return client.searchByJql(query, 0, 0).getTotal();
  }

  @Override
  public boolean hasNextIssues() {
    return currentIndex < finishIndex;
  }

  @Override
  public Collection<Issue> getNextIssues() {
    progressMonitor.startMeasuringTime();
    SearchResult searchResult = client.searchByJql(query, batchSize, currentIndex, fields);
    searchResult.getIssues().forEach(issue -> LOGGER.info("{}: {}", issue.getKey(), issue.getSummary()));
    currentIndex += batchSize;
    progressMonitor.endMeasuringTime();
    LOGGER.info("Progress {}/{}", (currentIndex > finishIndex) ? finishIndex : currentIndex, finishIndex);
    LOGGER.info("Remaining time: {}", progressMonitor.getFormattedRemainingTime((currentIndex > finishIndex) ? finishIndex : currentIndex));
    if (batchDelay != 0) {
      delay();
    }
    return CollectionsUtil.convert(searchResult.getIssues());
  }

  private void delay() {
    try {
      LOGGER.info("Waiting for " + batchDelay + "ms ...");
      Thread.sleep(batchDelay);
    } catch (InterruptedException e) {
      LOGGER.error("Delay between batch requests was interrupted.", e);
    }
  }
}