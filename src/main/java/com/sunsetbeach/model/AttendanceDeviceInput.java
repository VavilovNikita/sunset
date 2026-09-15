package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Full replacement on PATCH — no partial update (same convention as &#x60;PrinterInput&#x60;).
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class AttendanceDeviceInput {

  private String name;

  private String serial;

  private String address;

  private Integer port = 4370;

  private String timezone;

  private Boolean active = true;

  public AttendanceDeviceInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AttendanceDeviceInput(String name, String serial, String address, String timezone) {
    this.name = name;
    this.serial = serial;
    this.address = address;
    this.timezone = timezone;
  }

  public AttendanceDeviceInput name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(min = 1, max = 120) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public AttendanceDeviceInput serial(String serial) {
    this.serial = serial;
    return this;
  }

  /**
   * Get serial
   * @return serial
   */
  @NotNull @Size(min = 1, max = 120) 
  @JsonProperty("serial")
  public String getSerial() {
    return serial;
  }

  public void setSerial(String serial) {
    this.serial = serial;
  }

  public AttendanceDeviceInput address(String address) {
    this.address = address;
    return this;
  }

  /**
   * Get address
   * @return address
   */
  @NotNull @Size(min = 1, max = 255) 
  @JsonProperty("address")
  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public AttendanceDeviceInput port(Integer port) {
    this.port = port;
    return this;
  }

  /**
   * Get port
   * minimum: 1
   * maximum: 65535
   * @return port
   */
  @Min(1) @Max(65535) 
  @JsonProperty("port")
  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public AttendanceDeviceInput timezone(String timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * Get timezone
   * @return timezone
   */
  @NotNull @Size(min = 1, max = 100) 
  @JsonProperty("timezone")
  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public AttendanceDeviceInput active(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * Get active
   * @return active
   */
  
  @JsonProperty("active")
  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttendanceDeviceInput attendanceDeviceInput = (AttendanceDeviceInput) o;
    return Objects.equals(this.name, attendanceDeviceInput.name) &&
        Objects.equals(this.serial, attendanceDeviceInput.serial) &&
        Objects.equals(this.address, attendanceDeviceInput.address) &&
        Objects.equals(this.port, attendanceDeviceInput.port) &&
        Objects.equals(this.timezone, attendanceDeviceInput.timezone) &&
        Objects.equals(this.active, attendanceDeviceInput.active);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, serial, address, port, timezone, active);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AttendanceDeviceInput {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    serial: ").append(toIndentedString(serial)).append("\n");
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    port: ").append(toIndentedString(port)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
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

