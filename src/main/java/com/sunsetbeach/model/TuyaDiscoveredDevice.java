package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A device of the Tuya project. Only what the add screen needs - never Tuya&#39;s &#x60;localKey&#x60;.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class TuyaDiscoveredDevice {

  private String id;

  private String name;

  private String category;

  private JsonNullable<String> productName = JsonNullable.<String>undefined();

  private Boolean online;

  private Boolean sub;

  public TuyaDiscoveredDevice() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TuyaDiscoveredDevice(String id, String name, String category, Boolean online, Boolean sub) {
    this.id = id;
    this.name = name;
    this.category = category;
    this.online = online;
    this.sub = sub;
  }

  public TuyaDiscoveredDevice id(String id) {
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

  public TuyaDiscoveredDevice name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name the owner gave it in the Tuya app, or Tuya's own name when none.
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

  public TuyaDiscoveredDevice category(String category) {
    this.category = category;
    return this;
  }

  /**
   * Tuya category code: `kg` switch, `wnykq` IR hub, `qt` an IR remote (a virtual device under a hub), `wxkg` wireless switch.
   * @return category
   */
  @NotNull 
  @JsonProperty("category")
  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public TuyaDiscoveredDevice productName(String productName) {
    this.productName = JsonNullable.of(productName);
    return this;
  }

  /**
   * Get productName
   * @return productName
   */
  
  @JsonProperty("productName")
  public JsonNullable<String> getProductName() {
    return productName;
  }

  public void setProductName(JsonNullable<String> productName) {
    this.productName = productName;
  }

  public TuyaDiscoveredDevice online(Boolean online) {
    this.online = online;
    return this;
  }

  /**
   * Get online
   * @return online
   */
  @NotNull 
  @JsonProperty("online")
  public Boolean getOnline() {
    return online;
  }

  public void setOnline(Boolean online) {
    this.online = online;
  }

  public TuyaDiscoveredDevice sub(Boolean sub) {
    this.sub = sub;
    return this;
  }

  /**
   * A sub-device (for the gym, an IR remote such as an air conditioner).
   * @return sub
   */
  @NotNull 
  @JsonProperty("sub")
  public Boolean getSub() {
    return sub;
  }

  public void setSub(Boolean sub) {
    this.sub = sub;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TuyaDiscoveredDevice tuyaDiscoveredDevice = (TuyaDiscoveredDevice) o;
    return Objects.equals(this.id, tuyaDiscoveredDevice.id) &&
        Objects.equals(this.name, tuyaDiscoveredDevice.name) &&
        Objects.equals(this.category, tuyaDiscoveredDevice.category) &&
        equalsNullable(this.productName, tuyaDiscoveredDevice.productName) &&
        Objects.equals(this.online, tuyaDiscoveredDevice.online) &&
        Objects.equals(this.sub, tuyaDiscoveredDevice.sub);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, category, hashCodeNullable(productName), online, sub);
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
    sb.append("class TuyaDiscoveredDevice {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    productName: ").append(toIndentedString(productName)).append("\n");
    sb.append("    online: ").append(toIndentedString(online)).append("\n");
    sb.append("    sub: ").append(toIndentedString(sub)).append("\n");
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

