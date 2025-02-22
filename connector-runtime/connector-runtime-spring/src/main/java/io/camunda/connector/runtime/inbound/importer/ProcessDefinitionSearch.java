/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.connector.runtime.inbound.importer;

import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.exception.OperateException;
import io.camunda.operate.model.ProcessDefinition;
import io.camunda.operate.model.SearchResult;
import io.camunda.operate.search.SearchQuery;
import io.camunda.operate.search.Sort;
import io.camunda.operate.search.SortOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

/**
 * Stateful component that issues a process definition search based on the previous pagination
 * index.
 */
public class ProcessDefinitionSearch {

  private static final int PAGE_SIZE = 50;

  private static final Logger LOG = LoggerFactory.getLogger(ProcessDefinitionImporter.class);
  private final CamundaOperateClient camundaOperateClient;

  public ProcessDefinitionSearch(CamundaOperateClient camundaOperateClient) {
    this.camundaOperateClient = camundaOperateClient;
  }

  public void query(Consumer<List<ProcessDefinition>> resultHandler) {
    LOG.trace("Query process deployments...");
    List<ProcessDefinition> processDefinitions = new ArrayList<>();
    SearchResult<ProcessDefinition> processDefinitionResult;
    LOG.trace("Running paginated query");

    List<Object> paginationIndex = null;
    do {
      try {
        SearchQuery processDefinitionQuery =
            new SearchQuery.Builder()
                .searchAfter(paginationIndex)
                .sort(new Sort("key", SortOrder.DESC))
                .size(PAGE_SIZE)
                .build();
        processDefinitionResult =
            camundaOperateClient.searchProcessDefinitionResults(processDefinitionQuery);
      } catch (OperateException e) {
        throw new RuntimeException(e);
      }
      List<Object> newPaginationIdx = processDefinitionResult.getSortValues();

      if (!CollectionUtils.isEmpty(newPaginationIdx)) {
        paginationIndex = newPaginationIdx;
      }

      var items = processDefinitionResult.getItems();
      if (items != null) {
        processDefinitions.addAll(items);
      }

    } while (processDefinitionResult.getItems() != null
        && !processDefinitionResult.getItems().isEmpty());

    resultHandler.accept(processDefinitions);
  }
}
