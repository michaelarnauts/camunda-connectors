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
package io.camunda.connector.runtime.inbound.webhook;

import io.camunda.connector.api.inbound.Activity;
import io.camunda.connector.api.inbound.Severity;
import io.camunda.connector.runtime.inbound.lifecycle.ActiveInboundConnector;
import io.camunda.connector.runtime.inbound.webhook.model.CommonWebhookProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebhookConnectorRegistry {

  private static final String WEBHOOK_CONNECTOR_REGISTRY = "WebhookConnectorRegistry";
  public static final String DEPRECATED_WEBHOOK_MESSAGE_PREFIX = "Deprecated webhook path: ";
  private final Logger LOG = LoggerFactory.getLogger(WebhookConnectorRegistry.class);

  private final Map<String, ActiveInboundConnector> activeEndpointsByContext = new HashMap<>();

  // Reflect changes to this pattern in webhook element templates
  private static Pattern currentWebhookPathPattern =
      Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?$");

  public Optional<ActiveInboundConnector> getWebhookConnectorByContextPath(String context) {
    return Optional.ofNullable(activeEndpointsByContext.get(context));
  }

  public void register(ActiveInboundConnector connector) {
    var properties = connector.context().bindProperties(CommonWebhookProperties.class);
    var context = properties.getContext();

    logIfWebhookPathDeprecated(connector, context);

    var existingEndpoint = activeEndpointsByContext.putIfAbsent(context, connector);
    logIfEndpointExists(existingEndpoint, context);
  }

  private void logIfEndpointExists(ActiveInboundConnector existingEndpoint, String context) {
    if (existingEndpoint != null) {
      var bpmnProcessId = existingEndpoint.context().getDefinition().bpmnProcessId();
      var elementId = existingEndpoint.context().getDefinition().elementId();
      var logMessage =
          "Context: " + context + " already in use by " + bpmnProcessId + "/" + elementId + ".";
      LOG.debug(logMessage);
      throw new RuntimeException(logMessage);
    }
  }

  private static void logIfWebhookPathDeprecated(ActiveInboundConnector connector, String webhook) {

    if (!currentWebhookPathPattern.matcher(webhook).matches()) {
      String message = DEPRECATED_WEBHOOK_MESSAGE_PREFIX + webhook;

      connector
          .context()
          .log(Activity.level(Severity.WARNING).tag(WEBHOOK_CONNECTOR_REGISTRY).message(message));
    }
  }

  public void deregister(ActiveInboundConnector connector) {
    var context = connector.context().bindProperties(CommonWebhookProperties.class).getContext();
    activeEndpointsByContext.remove(context);
  }

  public void reset() {
    activeEndpointsByContext.clear();
  }
}
