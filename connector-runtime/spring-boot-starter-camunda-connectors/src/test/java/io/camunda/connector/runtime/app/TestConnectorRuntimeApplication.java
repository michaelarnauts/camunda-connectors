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
package io.camunda.connector.runtime.app;

import io.camunda.operate.CamundaOperateClient;
import io.camunda.zeebe.spring.client.configuration.CommonClientConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootApplication
@ImportAutoConfiguration({
  io.camunda.connector.runtime.InboundConnectorsAutoConfiguration.class,
  io.camunda.connector.runtime.OutboundConnectorsAutoConfiguration.class,
  io.camunda.connector.runtime.WebhookConnectorAutoConfiguration.class
})
@MockBean({CamundaOperateClient.class, CommonClientConfiguration.class})
public class TestConnectorRuntimeApplication {

  public static void main(String[] args) {
    SpringApplication.run(TestConnectorRuntimeApplication.class, args);
  }
}
