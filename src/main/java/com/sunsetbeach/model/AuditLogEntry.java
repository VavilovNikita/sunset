package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Role;
import java.time.OffsetDateTime;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One row from the append-only audit log - see the AuditLog tag description for what gets recorded and why. &#x60;summary&#x60; is written in plain language by the service action that triggered it, meant to be read directly by a hotel manager, not reconstructed by the client from raw before/after field values - deliberately not a generic entity diff (see the backend&#39;s &#x60;AuditLogService&#x60; for the reasoning: a diff of every field is both harder to read and, for the fields that actually matter here, no more precise than a sentence written at the one point in the code that already knows what changed and why it matters). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class AuditLogEntry {

  private String id;

  private String actorUserId;

  private String actorEmail;

  private Role actorRole;

  private AuditAction action;

  private AuditEntityType entityType;

  private JsonNullable<String> entityId = JsonNullable.<String>undefined();

  private String summary;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public AuditLogEntry() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AuditLogEntry(String id, String actorUserId, String actorEmail, Role actorRole, AuditAction action, AuditEntityType entityType, String summary, OffsetDateTime createdAt) {
    this.id = id;
    this.actorUserId = actorUserId;
    this.actorEmail = actorEmail;
    this.actorRole = actorRole;
    this.action = action;
    this.entityType = entityType;
    this.summary = summary;
    this.createdAt = createdAt;
  }

  public AuditLogEntry id(String id) {
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

  public AuditLogEntry actorUserId(String actorUserId) {
    this.actorUserId = actorUserId;
    return this;
  }

  /**
   * The acting user's id at the time of the action. Not guaranteed to still resolve via `GET /users/{id}` - see `actorEmail`.
   * @return actorUserId
   */
  @NotNull 
  @JsonProperty("actorUserId")
  public String getActorUserId() {
    return actorUserId;
  }

  public void setActorUserId(String actorUserId) {
    this.actorUserId = actorUserId;
  }

  public AuditLogEntry actorEmail(String actorEmail) {
    this.actorEmail = actorEmail;
    return this;
  }

  /**
   * The acting user's email *as it was at the time of the action* - a snapshot, not a live join to the current `User` row. This is deliberate: the acting user's account may since have had its email changed, or (if account deletion is ever added - today accounts are only disabled, never deleted) no longer exist at all, and this row must still say who did it. 
   * @return actorEmail
   */
  @NotNull 
  @JsonProperty("actorEmail")
  public String getActorEmail() {
    return actorEmail;
  }

  public void setActorEmail(String actorEmail) {
    this.actorEmail = actorEmail;
  }

  public AuditLogEntry actorRole(Role actorRole) {
    this.actorRole = actorRole;
    return this;
  }

  /**
   * Get actorRole
   * @return actorRole
   */
  @NotNull @Valid 
  @JsonProperty("actorRole")
  public Role getActorRole() {
    return actorRole;
  }

  public void setActorRole(Role actorRole) {
    this.actorRole = actorRole;
  }

  public AuditLogEntry action(AuditAction action) {
    this.action = action;
    return this;
  }

  /**
   * Get action
   * @return action
   */
  @NotNull @Valid 
  @JsonProperty("action")
  public AuditAction getAction() {
    return action;
  }

  public void setAction(AuditAction action) {
    this.action = action;
  }

  public AuditLogEntry entityType(AuditEntityType entityType) {
    this.entityType = entityType;
    return this;
  }

  /**
   * Get entityType
   * @return entityType
   */
  @NotNull @Valid 
  @JsonProperty("entityType")
  public AuditEntityType getEntityType() {
    return entityType;
  }

  public void setEntityType(AuditEntityType entityType) {
    this.entityType = entityType;
  }

  public AuditLogEntry entityId(String entityId) {
    this.entityId = JsonNullable.of(entityId);
    return this;
  }

  /**
   * Absent for an action with no single-record target (e.g. `BOOKINGS_EXPORTED`, a query over many bookings at once).
   * @return entityId
   */
  
  @JsonProperty("entityId")
  public JsonNullable<String> getEntityId() {
    return entityId;
  }

  public void setEntityId(JsonNullable<String> entityId) {
    this.entityId = entityId;
  }

  public AuditLogEntry summary(String summary) {
    this.summary = summary;
    return this;
  }

  /**
   * Human-readable description of what happened, written by the code that performed the action. Never contains a guest's email, phone, or payment note - see the tag description.
   * @return summary
   */
  @NotNull 
  @JsonProperty("summary")
  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public AuditLogEntry createdAt(OffsetDateTime createdAt) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuditLogEntry auditLogEntry = (AuditLogEntry) o;
    return Objects.equals(this.id, auditLogEntry.id) &&
        Objects.equals(this.actorUserId, auditLogEntry.actorUserId) &&
        Objects.equals(this.actorEmail, auditLogEntry.actorEmail) &&
        Objects.equals(this.actorRole, auditLogEntry.actorRole) &&
        Objects.equals(this.action, auditLogEntry.action) &&
        Objects.equals(this.entityType, auditLogEntry.entityType) &&
        equalsNullable(this.entityId, auditLogEntry.entityId) &&
        Objects.equals(this.summary, auditLogEntry.summary) &&
        Objects.equals(this.createdAt, auditLogEntry.createdAt);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, actorUserId, actorEmail, actorRole, action, entityType, hashCodeNullable(entityId), summary, createdAt);
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuditLogEntry {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    actorUserId: ").append(toIndentedString(actorUserId)).append("\n");
    sb.append("    actorEmail: ").append(toIndentedString(actorEmail)).append("\n");
    sb.append("    actorRole: ").append(toIndentedString(actorRole)).append("\n");
    sb.append("    action: ").append(toIndentedString(action)).append("\n");
    sb.append("    entityType: ").append(toIndentedString(entityType)).append("\n");
    sb.append("    entityId: ").append(toIndentedString(entityId)).append("\n");
    sb.append("    summary: ").append(toIndentedString(summary)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

