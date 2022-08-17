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
package io.camunda.connector.model;

import io.camunda.connector.api.ConnectorInput;
import io.camunda.connector.api.SecretStore;
import io.camunda.connector.api.Validator;
import java.util.Objects;

public class SqsConnectorRequest implements ConnectorInput {

  private AuthenticationRequestData authentication;
  private QueueRequestData queue;

  @Override
  public void validateWith(final Validator validator) {
    validator.require(authentication, "authentication");
    validator.require(queue, "queue");

    if (authentication != null) {
      authentication.validateWith(validator);
    }

    if (queue != null) {
      queue.validateWith(validator);
    }
  }

  @Override
  public void replaceSecrets(final SecretStore secretStore) {
    authentication.replaceSecrets(secretStore);
    queue.replaceSecrets(secretStore);
  }

  public AuthenticationRequestData getAuthentication() {
    return authentication;
  }

  public void setAuthentication(final AuthenticationRequestData authentication) {
    this.authentication = authentication;
  }

  public QueueRequestData getQueue() {
    return queue;
  }

  public void setQueue(final QueueRequestData queue) {
    this.queue = queue;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SqsConnectorRequest that = (SqsConnectorRequest) o;
    return authentication.equals(that.authentication) && queue.equals(that.queue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authentication, queue);
  }

  @Override
  public String toString() {
    return "SqsConnectorRequest{" + "authentication=" + authentication + ", queue=" + queue + '}';
  }
}
