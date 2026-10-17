package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * NotificationLinkCode
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class NotificationLinkCode {

  private String userId;

  private String url;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime expiresAt;

  public NotificationLinkCode() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NotificationLinkCode(String userId, String url, OffsetDateTime expiresAt) {
    this.userId = userId;
    this.url = url;
    this.expiresAt = expiresAt;
  }

  public NotificationLinkCode userId(String userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Get userId
   * @return userId
   */
  @NotNull 
  @JsonProperty("userId")
  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public NotificationLinkCode url(String url) {
    this.url = url;
    return this;
  }

  /**
   * `https://t.me/<bot>?start=<code>` - open it or show it as a QR. Single use, valid until `expiresAt`.
   * @return url
   */
  @NotNull 
  @JsonProperty("url")
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public NotificationLinkCode expiresAt(OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
    return this;
  }

  /**
   * Get expiresAt
   * @return expiresAt
   */
  @NotNull @Valid 
  @JsonProperty("expiresAt")
  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationLinkCode notificationLinkCode = (NotificationLinkCode) o;
    return Objects.equals(this.userId, notificationLinkCode.userId) &&
        Objects.equals(this.url, notificationLinkCode.url) &&
        Objects.equals(this.expiresAt, notificationLinkCode.expiresAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, url, expiresAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationLinkCode {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    expiresAt: ").append(toIndentedString(expiresAt)).append("\n");
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

