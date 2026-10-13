package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.GymAutomationDeviceResult;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * GymAutomationPhase
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymAutomationPhase {

  /**
   * `OK` - every device did what was needed; `PARTIAL` - some did and some failed (see the `FAILED` results); `FAILED` - none did; `SKIPPED` - nothing was needed or possible (see `skipReason`); `PENDING` - commands still being tried. 
   */
  public enum StateEnum {
    PENDING("PENDING"),
    
    OK("OK"),
    
    PARTIAL("PARTIAL"),
    
    FAILED("FAILED"),
    
    SKIPPED("SKIPPED");

    private String value;

    StateEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StateEnum fromValue(String value) {
      for (StateEnum b : StateEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private StateEnum state;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> at = JsonNullable.<OffsetDateTime>undefined();

  @Valid
  private List<@Valid GymAutomationDeviceResult> results = new ArrayList<>();

  public GymAutomationPhase() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GymAutomationPhase(StateEnum state, List<@Valid GymAutomationDeviceResult> results) {
    this.state = state;
    this.results = results;
  }

  public GymAutomationPhase state(StateEnum state) {
    this.state = state;
    return this;
  }

  /**
   * `OK` - every device did what was needed; `PARTIAL` - some did and some failed (see the `FAILED` results); `FAILED` - none did; `SKIPPED` - nothing was needed or possible (see `skipReason`); `PENDING` - commands still being tried. 
   * @return state
   */
  @NotNull 
  @JsonProperty("state")
  public StateEnum getState() {
    return state;
  }

  public void setState(StateEnum state) {
    this.state = state;
  }

  public GymAutomationPhase at(OffsetDateTime at) {
    this.at = JsonNullable.of(at);
    return this;
  }

  /**
   * When the phase finished (the last device's outcome).
   * @return at
   */
  @Valid 
  @JsonProperty("at")
  public JsonNullable<OffsetDateTime> getAt() {
    return at;
  }

  public void setAt(JsonNullable<OffsetDateTime> at) {
    this.at = at;
  }

  public GymAutomationPhase results(List<@Valid GymAutomationDeviceResult> results) {
    this.results = results;
    return this;
  }

  public GymAutomationPhase addResultsItem(GymAutomationDeviceResult resultsItem) {
    if (this.results == null) {
      this.results = new ArrayList<>();
    }
    this.results.add(resultsItem);
    return this;
  }

  /**
   * Get results
   * @return results
   */
  @NotNull @Valid 
  @JsonProperty("results")
  public List<@Valid GymAutomationDeviceResult> getResults() {
    return results;
  }

  public void setResults(List<@Valid GymAutomationDeviceResult> results) {
    this.results = results;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymAutomationPhase gymAutomationPhase = (GymAutomationPhase) o;
    return Objects.equals(this.state, gymAutomationPhase.state) &&
        equalsNullable(this.at, gymAutomationPhase.at) &&
        Objects.equals(this.results, gymAutomationPhase.results);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(state, hashCodeNullable(at), results);
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
    sb.append("class GymAutomationPhase {\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    at: ").append(toIndentedString(at)).append("\n");
    sb.append("    results: ").append(toIndentedString(results)).append("\n");
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

