package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * A fingerprint terminal (ZKTeco K60, TCP/IP port 4370) the server polls for its attendance log - the device never calls us. That direction is deliberate: there is no inbound endpoint this API exposes for a device to push to, so nothing on the internet, or even elsewhere on the hotel&#39;s own network, can reach the terminal or masquerade as it. The server needs only outbound reachability to &#x60;address:port&#x60; on the local network; deploying this feature adds no new attack surface to defend, unlike a push design, which would need an authenticated inbound listener. &#x60;lastSeenAt&#x60; is set on every successful poll and is not merely informational - a terminal that quietly stopped reporting looks, in the punch data alone, exactly like a month in which nobody worked, so a long gap here is the one signal that tells the difference. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class AttendanceDevice {

  private String id;

  private String name;

  private String serial;

  private String address;

  private Integer port;

  private String timezone;

  private Boolean active;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> lastSeenAt = JsonNullable.<OffsetDateTime>undefined();

  private Boolean windowedReadUnsupported;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public AttendanceDevice() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AttendanceDevice(String id, String name, String serial, String address, Integer port, String timezone, Boolean active, Boolean windowedReadUnsupported, OffsetDateTime createdAt) {
    this.id = id;
    this.name = name;
    this.serial = serial;
    this.address = address;
    this.port = port;
    this.timezone = timezone;
    this.active = active;
    this.windowedReadUnsupported = windowedReadUnsupported;
    this.createdAt = createdAt;
  }

  public AttendanceDevice id(String id) {
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

  public AttendanceDevice name(String name) {
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

  public AttendanceDevice serial(String serial) {
    this.serial = serial;
    return this;
  }

  /**
   * The device's own serial number - unique, so the same physical unit can't be registered twice.
   * @return serial
   */
  @NotNull 
  @JsonProperty("serial")
  public String getSerial() {
    return serial;
  }

  public void setSerial(String serial) {
    this.serial = serial;
  }

  public AttendanceDevice address(String address) {
    this.address = address;
    return this;
  }

  /**
   * IP address or hostname on the hotel's local network.
   * @return address
   */
  @NotNull 
  @JsonProperty("address")
  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public AttendanceDevice port(Integer port) {
    this.port = port;
    return this;
  }

  /**
   * Almost always 4370 on real hardware.
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

  public AttendanceDevice timezone(String timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * IANA zone id (e.g. `Asia/Bangkok`) for the device's own clock - not necessarily this server's. Set on every successful poll to match this server's clock, so drift never accumulates, but read here in the device's own zone for any punch read before that write lands. 
   * @return timezone
   */
  @NotNull 
  @JsonProperty("timezone")
  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public AttendanceDevice active(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * Whether this device is currently polled. A retired or replaced device is deactivated, not deleted, once it has ever produced a punch.
   * @return active
   */
  @NotNull 
  @JsonProperty("active")
  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public AttendanceDevice lastSeenAt(OffsetDateTime lastSeenAt) {
    this.lastSeenAt = JsonNullable.of(lastSeenAt);
    return this;
  }

  /**
   * When this device was last successfully polled. Null if it has never been reached.
   * @return lastSeenAt
   */
  @Valid 
  @JsonProperty("lastSeenAt")
  public JsonNullable<OffsetDateTime> getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(JsonNullable<OffsetDateTime> lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }

  public AttendanceDevice windowedReadUnsupported(Boolean windowedReadUnsupported) {
    this.windowedReadUnsupported = windowedReadUnsupported;
    return this;
  }

  /**
   * True if the most recent poll that actually tried a windowed read found this device rejecting it, so every poll since has been reading the entire log instead of a window - set from what the device answered, never a configuration guess, and cleared the moment a later poll succeeds with a window. False both when the device supports windowed reads and when no poll has tested it yet (a brand new device's first poll is always a full read regardless of support). 
   * @return windowedReadUnsupported
   */
  @NotNull 
  @JsonProperty("windowedReadUnsupported")
  public Boolean getWindowedReadUnsupported() {
    return windowedReadUnsupported;
  }

  public void setWindowedReadUnsupported(Boolean windowedReadUnsupported) {
    this.windowedReadUnsupported = windowedReadUnsupported;
  }

  public AttendanceDevice createdAt(OffsetDateTime createdAt) {
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
    AttendanceDevice attendanceDevice = (AttendanceDevice) o;
    return Objects.equals(this.id, attendanceDevice.id) &&
        Objects.equals(this.name, attendanceDevice.name) &&
        Objects.equals(this.serial, attendanceDevice.serial) &&
        Objects.equals(this.address, attendanceDevice.address) &&
        Objects.equals(this.port, attendanceDevice.port) &&
        Objects.equals(this.timezone, attendanceDevice.timezone) &&
        Objects.equals(this.active, attendanceDevice.active) &&
        equalsNullable(this.lastSeenAt, attendanceDevice.lastSeenAt) &&
        Objects.equals(this.windowedReadUnsupported, attendanceDevice.windowedReadUnsupported) &&
        Objects.equals(this.createdAt, attendanceDevice.createdAt);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, serial, address, port, timezone, active, hashCodeNullable(lastSeenAt), windowedReadUnsupported, createdAt);
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
    sb.append("class AttendanceDevice {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    serial: ").append(toIndentedString(serial)).append("\n");
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    port: ").append(toIndentedString(port)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    lastSeenAt: ").append(toIndentedString(lastSeenAt)).append("\n");
    sb.append("    windowedReadUnsupported: ").append(toIndentedString(windowedReadUnsupported)).append("\n");
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

