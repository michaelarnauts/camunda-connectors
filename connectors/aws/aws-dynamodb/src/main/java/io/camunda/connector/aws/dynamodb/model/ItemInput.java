/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.aws.dynamodb.model;

import io.camunda.connector.generator.java.annotation.TemplateDiscriminatorProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;

@TemplateSubType(label = "Item", id = "item")
@TemplateDiscriminatorProperty(name = "itemProp", group = "operation", label = "Select Action")
public sealed interface ItemInput extends AwsInput
    permits AddItem, DeleteItem, GetItem, UpdateItem {}
