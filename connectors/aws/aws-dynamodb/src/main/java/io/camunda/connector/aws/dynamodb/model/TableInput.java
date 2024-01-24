/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.aws.dynamodb.model;

import io.camunda.connector.generator.java.annotation.TemplateDiscriminatorProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;

@TemplateSubType(label = "Table", id = "table")
@TemplateDiscriminatorProperty(name = "table", group = "operation", label = "Select Action")
public sealed interface TableInput extends AwsInput
    permits CreateTable, DeleteTable, DescribeTable, ScanTable {}
