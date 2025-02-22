/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.sendgrid;

import java.util.Arrays;
import java.util.Objects;

public class ErrorResponse {
  private String message;
  private StackTraceElement[] stackTrace;

  public String getMessage() {
    return message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public StackTraceElement[] getStackTrace() {
    return stackTrace;
  }

  public void setStackTrace(final StackTraceElement[] stackTrace) {
    this.stackTrace = stackTrace;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ErrorResponse that = (ErrorResponse) o;
    return Objects.equals(message, that.message) && Arrays.equals(stackTrace, that.stackTrace);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(message);
    result = 31 * result + Arrays.hashCode(stackTrace);
    return result;
  }

  @Override
  public String toString() {
    return "ErrorResponse{"
        + "message='"
        + message
        + '\''
        + ", stackTrace="
        + Arrays.toString(stackTrace)
        + '}';
  }
}
