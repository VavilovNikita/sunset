package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.PrinterCodepage;
import com.sunsetbeach.model.PrinterDepartment;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Printer
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class Printer {

  private String id;

  private String name;

  private PrinterDepartment department;

  private String host;

  private Integer port;

  private PrinterCodepage codepage;

  private Boolean isActive;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastSentAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastFailedAt;

  public Printer() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Printer(String id, String name, PrinterDepartment department, String host, Integer port, PrinterCodepage codepage, Boolean isActive, OffsetDateTime createdAt) {
    this.id = id;
    this.name = name;
    this.department = department;
    this.host = host;
    this.port = port;
    this.codepage = codepage;
    this.isActive = isActive;
    this.createdAt = createdAt;
  }

  public Printer id(String id) {
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

  public Printer name(String name) {
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

  public Printer department(PrinterDepartment department) {
    this.department = department;
    return this;
  }

  /**
   * Get department
   * @return department
   */
  @NotNull @Valid 
  @JsonProperty("department")
  public PrinterDepartment getDepartment() {
    return department;
  }

  public void setDepartment(PrinterDepartment department) {
    this.department = department;
  }

  public Printer host(String host) {
    this.host = host;
    return this;
  }

  /**
   * IP address or hostname on the hotel's local network.
   * @return host
   */
  @NotNull 
  @JsonProperty("host")
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Printer port(Integer port) {
    this.port = port;
    return this;
  }

  /**
   * Almost always 9100 (raw ESC/POS-over-TCP) on real hardware.
   * @return port
   */
  @NotNull 
  @JsonProperty("port")
  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public Printer codepage(PrinterCodepage codepage) {
    this.codepage = codepage;
    return this;
  }

  /**
   * Get codepage
   * @return codepage
   */
  @NotNull @Valid 
  @JsonProperty("codepage")
  public PrinterCodepage getCodepage() {
    return codepage;
  }

  public void setCodepage(PrinterCodepage codepage) {
    this.codepage = codepage;
  }

  public Printer isActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether this is currently the live printer for its `department` - see `Printer_one_active_per_department`.
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

  public Printer createdAt(OffsetDateTime createdAt) {
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

  public Printer lastSentAt(OffsetDateTime lastSentAt) {
    this.lastSentAt = lastSentAt;
    return this;
  }

  /**
   * When this printer last accepted a job (the newest `SENT` print job's `updatedAt`). Derived from the print queue, not from a separate heartbeat: nothing polls a printer that has nothing to print, so a quiet printer simply has an old value here. Absent if it has never printed anything. 
   * @return lastSentAt
   */
  @Valid 
  @JsonProperty("lastSentAt")
  public OffsetDateTime getLastSentAt() {
    return lastSentAt;
  }

  public void setLastSentAt(OffsetDateTime lastSentAt) {
    this.lastSentAt = lastSentAt;
  }

  public Printer lastFailedAt(OffsetDateTime lastFailedAt) {
    this.lastFailedAt = lastFailedAt;
    return this;
  }

  /**
   * When a delivery attempt to this printer last failed (the newest `updatedAt` among its non-dismissed `PENDING`/`FAILED` jobs that carry a `lastError`). Dismissed jobs are left out because dismissing one also touches its `updatedAt`. Newer than `lastSentAt` means the printer is not answering right now. Absent if no undismissed attempt has failed. 
   * @return lastFailedAt
   */
  @Valid 
  @JsonProperty("lastFailedAt")
  public OffsetDateTime getLastFailedAt() {
    return lastFailedAt;
  }

  public void setLastFailedAt(OffsetDateTime lastFailedAt) {
    this.lastFailedAt = lastFailedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Printer printer = (Printer) o;
    return Objects.equals(this.id, printer.id) &&
        Objects.equals(this.name, printer.name) &&
        Objects.equals(this.department, printer.department) &&
        Objects.equals(this.host, printer.host) &&
        Objects.equals(this.port, printer.port) &&
        Objects.equals(this.codepage, printer.codepage) &&
        Objects.equals(this.isActive, printer.isActive) &&
        Objects.equals(this.createdAt, printer.createdAt) &&
        Objects.equals(this.lastSentAt, printer.lastSentAt) &&
        Objects.equals(this.lastFailedAt, printer.lastFailedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, department, host, port, codepage, isActive, createdAt, lastSentAt, lastFailedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Printer {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    department: ").append(toIndentedString(department)).append("\n");
    sb.append("    host: ").append(toIndentedString(host)).append("\n");
    sb.append("    port: ").append(toIndentedString(port)).append("\n");
    sb.append("    codepage: ").append(toIndentedString(codepage)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    lastSentAt: ").append(toIndentedString(lastSentAt)).append("\n");
    sb.append("    lastFailedAt: ").append(toIndentedString(lastFailedAt)).append("\n");
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

