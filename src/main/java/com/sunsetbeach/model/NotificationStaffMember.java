package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.model.Role;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * NotificationStaffMember
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class NotificationStaffMember {

  private String userId;

  private String name;

  private Role role;

  private Boolean isActive;

  private NotificationLanguage language;

  private Boolean telegramLinked;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime telegramLinkedAt;

  public NotificationStaffMember() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NotificationStaffMember(String userId, String name, Role role, Boolean isActive, Boolean telegramLinked) {
    this.userId = userId;
    this.name = name;
    this.role = role;
    this.isActive = isActive;
    this.telegramLinked = telegramLinked;
  }

  public NotificationStaffMember userId(String userId) {
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

  public NotificationStaffMember name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public NotificationStaffMember role(Role role) {
    this.role = role;
    return this;
  }

  /**
   * Get role
   * @return role
   */
  @NotNull @Valid 
  @JsonProperty("role")
  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public NotificationStaffMember isActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Get isActive
   * @return isActive
   */
  @NotNull 
  @JsonProperty("isActive")
  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  public NotificationStaffMember language(NotificationLanguage language) {
    this.language = language;
    return this;
  }

  /**
   * Get language
   * @return language
   */
  @Valid 
  @JsonProperty("language")
  public NotificationLanguage getLanguage() {
    return language;
  }

  public void setLanguage(NotificationLanguage language) {
    this.language = language;
  }

  public NotificationStaffMember telegramLinked(Boolean telegramLinked) {
    this.telegramLinked = telegramLinked;
    return this;
  }

  /**
   * Get telegramLinked
   * @return telegramLinked
   */
  @NotNull 
  @JsonProperty("telegramLinked")
  public Boolean getTelegramLinked() {
    return telegramLinked;
  }

  public void setTelegramLinked(Boolean telegramLinked) {
    this.telegramLinked = telegramLinked;
  }

  public NotificationStaffMember telegramLinkedAt(OffsetDateTime telegramLinkedAt) {
    this.telegramLinkedAt = telegramLinkedAt;
    return this;
  }

  /**
   * When the chat was linked; absent when not linked.
   * @return telegramLinkedAt
   */
  @Valid 
  @JsonProperty("telegramLinkedAt")
  public OffsetDateTime getTelegramLinkedAt() {
    return telegramLinkedAt;
  }

  public void setTelegramLinkedAt(OffsetDateTime telegramLinkedAt) {
    this.telegramLinkedAt = telegramLinkedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationStaffMember notificationStaffMember = (NotificationStaffMember) o;
    return Objects.equals(this.userId, notificationStaffMember.userId) &&
        Objects.equals(this.name, notificationStaffMember.name) &&
        Objects.equals(this.role, notificationStaffMember.role) &&
        Objects.equals(this.isActive, notificationStaffMember.isActive) &&
        Objects.equals(this.language, notificationStaffMember.language) &&
        Objects.equals(this.telegramLinked, notificationStaffMember.telegramLinked) &&
        Objects.equals(this.telegramLinkedAt, notificationStaffMember.telegramLinkedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, name, role, isActive, language, telegramLinked, telegramLinkedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationStaffMember {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    language: ").append(toIndentedString(language)).append("\n");
    sb.append("    telegramLinked: ").append(toIndentedString(telegramLinked)).append("\n");
    sb.append("    telegramLinkedAt: ").append(toIndentedString(telegramLinkedAt)).append("\n");
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

