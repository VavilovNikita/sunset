package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.ShiftChangeRequestChannel;
import com.sunsetbeach.model.ShiftChangeRequestStatus;
import com.sunsetbeach.model.ShiftChangeRequestType;
import com.sunsetbeach.model.ShiftRequestCoverage;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ShiftChangeRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ShiftChangeRequest {

  private String id;

  private String employeeUserId;

  private String employeeName;

  private String rosterEntryId;

  private ShiftChangeRequestType type;

  private ShiftChangeRequestStatus status;

  private ShiftChangeRequestChannel channel;

  private String reason;

  private String shiftDate;

  private String shiftCode;

  private String shiftTimes;

  private String targetDate;

  private ShiftRequestCoverage coverage;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  private String decidedByUserId;

  private String decidedByName;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime decidedAt;

  private String decisionComment;

  private String expiredReason;

  private Boolean callerMayDecide;

  public ShiftChangeRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ShiftChangeRequest(String id, String employeeUserId, String employeeName, ShiftChangeRequestType type, ShiftChangeRequestStatus status, ShiftChangeRequestChannel channel, String shiftDate, String shiftCode, OffsetDateTime createdAt, Boolean callerMayDecide) {
    this.id = id;
    this.employeeUserId = employeeUserId;
    this.employeeName = employeeName;
    this.type = type;
    this.status = status;
    this.channel = channel;
    this.shiftDate = shiftDate;
    this.shiftCode = shiftCode;
    this.createdAt = createdAt;
    this.callerMayDecide = callerMayDecide;
  }

  public ShiftChangeRequest id(String id) {
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

  public ShiftChangeRequest employeeUserId(String employeeUserId) {
    this.employeeUserId = employeeUserId;
    return this;
  }

  /**
   * Get employeeUserId
   * @return employeeUserId
   */
  @NotNull 
  @JsonProperty("employeeUserId")
  public String getEmployeeUserId() {
    return employeeUserId;
  }

  public void setEmployeeUserId(String employeeUserId) {
    this.employeeUserId = employeeUserId;
  }

  public ShiftChangeRequest employeeName(String employeeName) {
    this.employeeName = employeeName;
    return this;
  }

  /**
   * Get employeeName
   * @return employeeName
   */
  @NotNull 
  @JsonProperty("employeeName")
  public String getEmployeeName() {
    return employeeName;
  }

  public void setEmployeeName(String employeeName) {
    this.employeeName = employeeName;
  }

  public ShiftChangeRequest rosterEntryId(String rosterEntryId) {
    this.rosterEntryId = rosterEntryId;
    return this;
  }

  /**
   * The entry concerned; absent once that entry has been deleted.
   * @return rosterEntryId
   */
  
  @JsonProperty("rosterEntryId")
  public String getRosterEntryId() {
    return rosterEntryId;
  }

  public void setRosterEntryId(String rosterEntryId) {
    this.rosterEntryId = rosterEntryId;
  }

  public ShiftChangeRequest type(ShiftChangeRequestType type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @JsonProperty("type")
  public ShiftChangeRequestType getType() {
    return type;
  }

  public void setType(ShiftChangeRequestType type) {
    this.type = type;
  }

  public ShiftChangeRequest status(ShiftChangeRequestStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public ShiftChangeRequestStatus getStatus() {
    return status;
  }

  public void setStatus(ShiftChangeRequestStatus status) {
    this.status = status;
  }

  public ShiftChangeRequest channel(ShiftChangeRequestChannel channel) {
    this.channel = channel;
    return this;
  }

  /**
   * Get channel
   * @return channel
   */
  @NotNull @Valid 
  @JsonProperty("channel")
  public ShiftChangeRequestChannel getChannel() {
    return channel;
  }

  public void setChannel(ShiftChangeRequestChannel channel) {
    this.channel = channel;
  }

  public ShiftChangeRequest reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * The employee's reason (up to 300 characters). Absent when none was required or given.
   * @return reason
   */
  
  @JsonProperty("reason")
  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public ShiftChangeRequest shiftDate(String shiftDate) {
    this.shiftDate = shiftDate;
    return this;
  }

  /**
   * The date of the shift when the request was filed.
   * @return shiftDate
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("shiftDate")
  public String getShiftDate() {
    return shiftDate;
  }

  public void setShiftDate(String shiftDate) {
    this.shiftDate = shiftDate;
  }

  public ShiftChangeRequest shiftCode(String shiftCode) {
    this.shiftCode = shiftCode;
    return this;
  }

  /**
   * The shift code's text when the request was filed (a snapshot).
   * @return shiftCode
   */
  @NotNull 
  @JsonProperty("shiftCode")
  public String getShiftCode() {
    return shiftCode;
  }

  public void setShiftCode(String shiftCode) {
    this.shiftCode = shiftCode;
  }

  public ShiftChangeRequest shiftTimes(String shiftTimes) {
    this.shiftTimes = shiftTimes;
    return this;
  }

  /**
   * The shift's intervals when the request was filed, like `09:00-18:00` or `09:00-13:00, 17:00-21:00`. Absent for a code without times.
   * @return shiftTimes
   */
  
  @JsonProperty("shiftTimes")
  public String getShiftTimes() {
    return shiftTimes;
  }

  public void setShiftTimes(String shiftTimes) {
    this.shiftTimes = shiftTimes;
  }

  public ShiftChangeRequest targetDate(String targetDate) {
    this.targetDate = targetDate;
    return this;
  }

  /**
   * MOVE only.
   * @return targetDate
   */
  @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("targetDate")
  public String getTargetDate() {
    return targetDate;
  }

  public void setTargetDate(String targetDate) {
    this.targetDate = targetDate;
  }

  public ShiftChangeRequest coverage(ShiftRequestCoverage coverage) {
    this.coverage = coverage;
    return this;
  }

  /**
   * Present only while the request is `PENDING` (what a decision would do to the department *now*; a settled request has no such figure), and only when the employee has a department with a minimum. 
   * @return coverage
   */
  @Valid 
  @JsonProperty("coverage")
  public ShiftRequestCoverage getCoverage() {
    return coverage;
  }

  public void setCoverage(ShiftRequestCoverage coverage) {
    this.coverage = coverage;
  }

  public ShiftChangeRequest createdAt(OffsetDateTime createdAt) {
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

  public ShiftChangeRequest decidedByUserId(String decidedByUserId) {
    this.decidedByUserId = decidedByUserId;
    return this;
  }

  /**
   * Get decidedByUserId
   * @return decidedByUserId
   */
  
  @JsonProperty("decidedByUserId")
  public String getDecidedByUserId() {
    return decidedByUserId;
  }

  public void setDecidedByUserId(String decidedByUserId) {
    this.decidedByUserId = decidedByUserId;
  }

  public ShiftChangeRequest decidedByName(String decidedByName) {
    this.decidedByName = decidedByName;
    return this;
  }

  /**
   * Get decidedByName
   * @return decidedByName
   */
  
  @JsonProperty("decidedByName")
  public String getDecidedByName() {
    return decidedByName;
  }

  public void setDecidedByName(String decidedByName) {
    this.decidedByName = decidedByName;
  }

  public ShiftChangeRequest decidedAt(OffsetDateTime decidedAt) {
    this.decidedAt = decidedAt;
    return this;
  }

  /**
   * Get decidedAt
   * @return decidedAt
   */
  @Valid 
  @JsonProperty("decidedAt")
  public OffsetDateTime getDecidedAt() {
    return decidedAt;
  }

  public void setDecidedAt(OffsetDateTime decidedAt) {
    this.decidedAt = decidedAt;
  }

  public ShiftChangeRequest decisionComment(String decisionComment) {
    this.decisionComment = decisionComment;
    return this;
  }

  /**
   * Get decisionComment
   * @return decisionComment
   */
  
  @JsonProperty("decisionComment")
  public String getDecisionComment() {
    return decisionComment;
  }

  public void setDecisionComment(String decisionComment) {
    this.decisionComment = decisionComment;
  }

  public ShiftChangeRequest expiredReason(String expiredReason) {
    this.expiredReason = expiredReason;
    return this;
  }

  /**
   * Our own words for why a request became `EXPIRED`.
   * @return expiredReason
   */
  
  @JsonProperty("expiredReason")
  public String getExpiredReason() {
    return expiredReason;
  }

  public void setExpiredReason(String expiredReason) {
    this.expiredReason = expiredReason;
  }

  public ShiftChangeRequest callerMayDecide(Boolean callerMayDecide) {
    this.callerMayDecide = callerMayDecide;
    return this;
  }

  /**
   * True when the caller is one of today's deciders and the request is still `PENDING`.
   * @return callerMayDecide
   */
  @NotNull 
  @JsonProperty("callerMayDecide")
  public Boolean getCallerMayDecide() {
    return callerMayDecide;
  }

  public void setCallerMayDecide(Boolean callerMayDecide) {
    this.callerMayDecide = callerMayDecide;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftChangeRequest shiftChangeRequest = (ShiftChangeRequest) o;
    return Objects.equals(this.id, shiftChangeRequest.id) &&
        Objects.equals(this.employeeUserId, shiftChangeRequest.employeeUserId) &&
        Objects.equals(this.employeeName, shiftChangeRequest.employeeName) &&
        Objects.equals(this.rosterEntryId, shiftChangeRequest.rosterEntryId) &&
        Objects.equals(this.type, shiftChangeRequest.type) &&
        Objects.equals(this.status, shiftChangeRequest.status) &&
        Objects.equals(this.channel, shiftChangeRequest.channel) &&
        Objects.equals(this.reason, shiftChangeRequest.reason) &&
        Objects.equals(this.shiftDate, shiftChangeRequest.shiftDate) &&
        Objects.equals(this.shiftCode, shiftChangeRequest.shiftCode) &&
        Objects.equals(this.shiftTimes, shiftChangeRequest.shiftTimes) &&
        Objects.equals(this.targetDate, shiftChangeRequest.targetDate) &&
        Objects.equals(this.coverage, shiftChangeRequest.coverage) &&
        Objects.equals(this.createdAt, shiftChangeRequest.createdAt) &&
        Objects.equals(this.decidedByUserId, shiftChangeRequest.decidedByUserId) &&
        Objects.equals(this.decidedByName, shiftChangeRequest.decidedByName) &&
        Objects.equals(this.decidedAt, shiftChangeRequest.decidedAt) &&
        Objects.equals(this.decisionComment, shiftChangeRequest.decisionComment) &&
        Objects.equals(this.expiredReason, shiftChangeRequest.expiredReason) &&
        Objects.equals(this.callerMayDecide, shiftChangeRequest.callerMayDecide);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, employeeUserId, employeeName, rosterEntryId, type, status, channel, reason, shiftDate, shiftCode, shiftTimes, targetDate, coverage, createdAt, decidedByUserId, decidedByName, decidedAt, decisionComment, expiredReason, callerMayDecide);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShiftChangeRequest {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    employeeUserId: ").append(toIndentedString(employeeUserId)).append("\n");
    sb.append("    employeeName: ").append(toIndentedString(employeeName)).append("\n");
    sb.append("    rosterEntryId: ").append(toIndentedString(rosterEntryId)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    shiftDate: ").append(toIndentedString(shiftDate)).append("\n");
    sb.append("    shiftCode: ").append(toIndentedString(shiftCode)).append("\n");
    sb.append("    shiftTimes: ").append(toIndentedString(shiftTimes)).append("\n");
    sb.append("    targetDate: ").append(toIndentedString(targetDate)).append("\n");
    sb.append("    coverage: ").append(toIndentedString(coverage)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    decidedByUserId: ").append(toIndentedString(decidedByUserId)).append("\n");
    sb.append("    decidedByName: ").append(toIndentedString(decidedByName)).append("\n");
    sb.append("    decidedAt: ").append(toIndentedString(decidedAt)).append("\n");
    sb.append("    decisionComment: ").append(toIndentedString(decisionComment)).append("\n");
    sb.append("    expiredReason: ").append(toIndentedString(expiredReason)).append("\n");
    sb.append("    callerMayDecide: ").append(toIndentedString(callerMayDecide)).append("\n");
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

