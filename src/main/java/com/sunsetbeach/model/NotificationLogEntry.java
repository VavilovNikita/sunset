package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.NotificationDeliveryStatus;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.model.NotificationType;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * NotificationLogEntry
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class NotificationLogEntry {

  private String id;

  private NotificationType type;

  private String recipientUserId;

  private String recipientName;

  private NotificationLanguage language;

  private String text;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  private NotificationDeliveryStatus deliveryStatus;

  private Integer attempts;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime sendAfter;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime sentAt;

  private String error;

  public NotificationLogEntry() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NotificationLogEntry(String id, NotificationType type, String recipientUserId, String recipientName, NotificationLanguage language, String text, OffsetDateTime createdAt, NotificationDeliveryStatus deliveryStatus, Integer attempts) {
    this.id = id;
    this.type = type;
    this.recipientUserId = recipientUserId;
    this.recipientName = recipientName;
    this.language = language;
    this.text = text;
    this.createdAt = createdAt;
    this.deliveryStatus = deliveryStatus;
    this.attempts = attempts;
  }

  public NotificationLogEntry id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public NotificationLogEntry type(NotificationType type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @JsonProperty("type")
  public NotificationType getType() {
    return type;
  }

  public void setType(NotificationType type) {
    this.type = type;
  }

  public NotificationLogEntry recipientUserId(String recipientUserId) {
    this.recipientUserId = recipientUserId;
    return this;
  }

  /**
   * Get recipientUserId
   * @return recipientUserId
   */
  @NotNull 
  @JsonProperty("recipientUserId")
  public String getRecipientUserId() {
    return recipientUserId;
  }

  public void setRecipientUserId(String recipientUserId) {
    this.recipientUserId = recipientUserId;
  }

  public NotificationLogEntry recipientName(String recipientName) {
    this.recipientName = recipientName;
    return this;
  }

  /**
   * Get recipientName
   * @return recipientName
   */
  @NotNull 
  @JsonProperty("recipientName")
  public String getRecipientName() {
    return recipientName;
  }

  public void setRecipientName(String recipientName) {
    this.recipientName = recipientName;
  }

  public NotificationLogEntry language(NotificationLanguage language) {
    this.language = language;
    return this;
  }

  /**
   * Get language
   * @return language
   */
  @NotNull @Valid 
  @JsonProperty("language")
  public NotificationLanguage getLanguage() {
    return language;
  }

  public void setLanguage(NotificationLanguage language) {
    this.language = language;
  }

  public NotificationLogEntry text(String text) {
    this.text = text;
    return this;
  }

  /**
   * The message exactly as it was written for the recipient.
   * @return text
   */
  @NotNull 
  @JsonProperty("text")
  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public NotificationLogEntry createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public NotificationLogEntry deliveryStatus(NotificationDeliveryStatus deliveryStatus) {
    this.deliveryStatus = deliveryStatus;
    return this;
  }

  /**
   * Get deliveryStatus
   * @return deliveryStatus
   */
  @NotNull @Valid 
  @JsonProperty("deliveryStatus")
  public NotificationDeliveryStatus getDeliveryStatus() {
    return deliveryStatus;
  }

  public void setDeliveryStatus(NotificationDeliveryStatus deliveryStatus) {
    this.deliveryStatus = deliveryStatus;
  }

  public NotificationLogEntry attempts(Integer attempts) {
    this.attempts = attempts;
    return this;
  }

  /**
   * Get attempts
   * @return attempts
   */
  @NotNull 
  @JsonProperty("attempts")
  public Integer getAttempts() {
    return attempts;
  }

  public void setAttempts(Integer attempts) {
    this.attempts = attempts;
  }

  public NotificationLogEntry sendAfter(OffsetDateTime sendAfter) {
    this.sendAfter = sendAfter;
    return this;
  }

  /**
   * While `PENDING`, the earliest moment it will be sent (the end of quiet hours, or the next retry).
   * @return sendAfter
   */
  @Valid 
  @JsonProperty("sendAfter")
  public OffsetDateTime getSendAfter() {
    return sendAfter;
  }

  public void setSendAfter(OffsetDateTime sendAfter) {
    this.sendAfter = sendAfter;
  }

  public NotificationLogEntry sentAt(OffsetDateTime sentAt) {
    this.sentAt = sentAt;
    return this;
  }

  /**
   * Get sentAt
   * @return sentAt
   */
  @Valid 
  @JsonProperty("sentAt")
  public OffsetDateTime getSentAt() {
    return sentAt;
  }

  public void setSentAt(OffsetDateTime sentAt) {
    this.sentAt = sentAt;
  }

  public NotificationLogEntry error(String error) {
    this.error = error;
    return this;
  }

  /**
   * Why the last attempt failed, in our own words.
   * @return error
   */
  
  @JsonProperty("error")
  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationLogEntry notificationLogEntry = (NotificationLogEntry) o;
    return Objects.equals(this.id, notificationLogEntry.id) &&
        Objects.equals(this.type, notificationLogEntry.type) &&
        Objects.equals(this.recipientUserId, notificationLogEntry.recipientUserId) &&
        Objects.equals(this.recipientName, notificationLogEntry.recipientName) &&
        Objects.equals(this.language, notificationLogEntry.language) &&
        Objects.equals(this.text, notificationLogEntry.text) &&
        Objects.equals(this.createdAt, notificationLogEntry.createdAt) &&
        Objects.equals(this.deliveryStatus, notificationLogEntry.deliveryStatus) &&
        Objects.equals(this.attempts, notificationLogEntry.attempts) &&
        Objects.equals(this.sendAfter, notificationLogEntry.sendAfter) &&
        Objects.equals(this.sentAt, notificationLogEntry.sentAt) &&
        Objects.equals(this.error, notificationLogEntry.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, recipientUserId, recipientName, language, text, createdAt, deliveryStatus, attempts, sendAfter, sentAt, error);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationLogEntry {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    recipientUserId: ").append(toIndentedString(recipientUserId)).append("\n");
    sb.append("    recipientName: ").append(toIndentedString(recipientName)).append("\n");
    sb.append("    language: ").append(toIndentedString(language)).append("\n");
    sb.append("    text: ").append(toIndentedString(text)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    deliveryStatus: ").append(toIndentedString(deliveryStatus)).append("\n");
    sb.append("    attempts: ").append(toIndentedString(attempts)).append("\n");
    sb.append("    sendAfter: ").append(toIndentedString(sendAfter)).append("\n");
    sb.append("    sentAt: ").append(toIndentedString(sentAt)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

