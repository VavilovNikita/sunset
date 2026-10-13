package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.GymAutomationPhase;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * What the equipment automation (&#x60;GymAutomationService&#x60;) did around a reservation. &#x60;switchOn&#x60; is the switch-on that happens &#x60;leadMinutes&#x60; before the start, &#x60;switchOff&#x60; the switch-off at the end (or after a cancellation). A phase is absent until the automation reaches it. The automation only ever sends commands at these transitions - it never reads device state and never overrides a manual change in between. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GymAutomation {

  private GymAutomationPhase switchOn;

  private GymAutomationPhase switchOff;

  public GymAutomation switchOn(GymAutomationPhase switchOn) {
    this.switchOn = switchOn;
    return this;
  }

  /**
   * Get switchOn
   * @return switchOn
   */
  @Valid 
  @JsonProperty("switchOn")
  public GymAutomationPhase getSwitchOn() {
    return switchOn;
  }

  public void setSwitchOn(GymAutomationPhase switchOn) {
    this.switchOn = switchOn;
  }

  public GymAutomation switchOff(GymAutomationPhase switchOff) {
    this.switchOff = switchOff;
    return this;
  }

  /**
   * Get switchOff
   * @return switchOff
   */
  @Valid 
  @JsonProperty("switchOff")
  public GymAutomationPhase getSwitchOff() {
    return switchOff;
  }

  public void setSwitchOff(GymAutomationPhase switchOff) {
    this.switchOff = switchOff;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GymAutomation gymAutomation = (GymAutomation) o;
    return Objects.equals(this.switchOn, gymAutomation.switchOn) &&
        Objects.equals(this.switchOff, gymAutomation.switchOff);
  }

  @Override
  public int hashCode() {
    return Objects.hash(switchOn, switchOff);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GymAutomation {\n");
    sb.append("    switchOn: ").append(toIndentedString(switchOn)).append("\n");
    sb.append("    switchOff: ").append(toIndentedString(switchOff)).append("\n");
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

