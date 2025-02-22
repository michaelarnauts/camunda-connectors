/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.model.request.chat;

import com.microsoft.graph.requests.GraphServiceClient;
import io.camunda.connector.model.request.MSTeamsRequestData;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import okhttp3.Request;

public class ListChatMembers extends MSTeamsRequestData {

  @NotBlank private String chatId;

  @Override
  public Object invoke(final GraphServiceClient<Request> graphClient) {
    return graphClient.chats(chatId).members().buildRequest().get();
  }

  public String getChatId() {
    return chatId;
  }

  public void setChatId(final String chatId) {
    this.chatId = chatId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ListChatMembers that = (ListChatMembers) o;
    return Objects.equals(chatId, that.chatId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chatId);
  }

  @Override
  public String toString() {
    return "ListChatMembers{" + "chatId='" + chatId + "'" + "}";
  }
}
