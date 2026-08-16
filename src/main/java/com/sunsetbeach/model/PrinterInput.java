package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.PrinterCodepage;
import com.sunsetbeach.model.PrinterDepartment;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Full replacement on PATCH — no partial update (same convention as &#x60;MenuItemInput&#x60;/&#x60;TableInput&#x60;).
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-16T20:25:37.858402100+03:00[Europe/Moscow]", comments = "Generator version: 7.10.0")
public class PrinterInput {

  private String name;

  private PrinterDepartment department;

  private String host;

  private Integer port = 9100;

  private PrinterCodepage codepage;

  private Boolean isActive = true;

  public PrinterInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PrinterInput(String name, PrinterDepartment department, String host) {
    this.name = name;
    this.department = department;
    this.host = host;
  }

  public PrinterInput name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(min = 2, max = 120) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PrinterInput department(PrinterDepartment department) {
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

  public PrinterInput host(String host) {
    this.host = host;
    return this;
  }

  /**
   * Get host
   * @return host
   */
  @NotNull @Size(min = 1, max = 255) 
  @JsonProperty("host")
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public PrinterInput port(Integer port) {
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

  public PrinterInput codepage(PrinterCodepage codepage) {
    this.codepage = codepage;
    return this;
  }

  /**
   * Get codepage
   * @return codepage
   */
  @Valid 
  @JsonProperty("codepage")
  public PrinterCodepage getCodepage() {
    return codepage;
  }

  public void setCodepage(PrinterCodepage codepage) {
    this.codepage = codepage;
  }

  public PrinterInput isActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Get isActive
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrinterInput printerInput = (PrinterInput) o;
    return Objects.equals(this.name, printerInput.name) &&
        Objects.equals(this.department, printerInput.department) &&
        Objects.equals(this.host, printerInput.host) &&
        Objects.equals(this.port, printerInput.port) &&
        Objects.equals(this.codepage, printerInput.codepage) &&
        Objects.equals(this.isActive, printerInput.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, department, host, port, codepage, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PrinterInput {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    department: ").append(toIndentedString(department)).append("\n");
    sb.append("    host: ").append(toIndentedString(host)).append("\n");
    sb.append("    port: ").append(toIndentedString(port)).append("\n");
    sb.append("    codepage: ").append(toIndentedString(codepage)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

