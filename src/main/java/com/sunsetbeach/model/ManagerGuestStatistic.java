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
 * ManagerGuestStatistic
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ManagerGuestStatistic {

  private Integer adultsInHouse;

  private Integer childrenInHouse;

  private Integer guestsInHouse;

  private JsonNullable<String> averageGuestsPerRoom = JsonNullable.<String>undefined();

  private JsonNullable<String> averageRatePerGuest = JsonNullable.<String>undefined();

  private JsonNullable<String> averageLengthOfStay = JsonNullable.<String>undefined();

  private Integer complimentaryGuests;

  private Integer houseUseGuests;

  public ManagerGuestStatistic() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ManagerGuestStatistic(Integer adultsInHouse, Integer childrenInHouse, Integer guestsInHouse, String averageGuestsPerRoom, String averageRatePerGuest, String averageLengthOfStay, Integer complimentaryGuests, Integer houseUseGuests) {
    this.adultsInHouse = adultsInHouse;
    this.childrenInHouse = childrenInHouse;
    this.guestsInHouse = guestsInHouse;
    this.averageGuestsPerRoom = JsonNullable.of(averageGuestsPerRoom);
    this.averageRatePerGuest = JsonNullable.of(averageRatePerGuest);
    this.averageLengthOfStay = JsonNullable.of(averageLengthOfStay);
    this.complimentaryGuests = complimentaryGuests;
    this.houseUseGuests = houseUseGuests;
  }

  public ManagerGuestStatistic adultsInHouse(Integer adultsInHouse) {
    this.adultsInHouse = adultsInHouse;
    return this;
  }

  /**
   * Get adultsInHouse
   * @return adultsInHouse
   */
  @NotNull 
  @JsonProperty("adultsInHouse")
  public Integer getAdultsInHouse() {
    return adultsInHouse;
  }

  public void setAdultsInHouse(Integer adultsInHouse) {
    this.adultsInHouse = adultsInHouse;
  }

  public ManagerGuestStatistic childrenInHouse(Integer childrenInHouse) {
    this.childrenInHouse = childrenInHouse;
    return this;
  }

  /**
   * Get childrenInHouse
   * @return childrenInHouse
   */
  @NotNull 
  @JsonProperty("childrenInHouse")
  public Integer getChildrenInHouse() {
    return childrenInHouse;
  }

  public void setChildrenInHouse(Integer childrenInHouse) {
    this.childrenInHouse = childrenInHouse;
  }

  public ManagerGuestStatistic guestsInHouse(Integer guestsInHouse) {
    this.guestsInHouse = guestsInHouse;
    return this;
  }

  /**
   * Get guestsInHouse
   * @return guestsInHouse
   */
  @NotNull 
  @JsonProperty("guestsInHouse")
  public Integer getGuestsInHouse() {
    return guestsInHouse;
  }

  public void setGuestsInHouse(Integer guestsInHouse) {
    this.guestsInHouse = guestsInHouse;
  }

  public ManagerGuestStatistic averageGuestsPerRoom(String averageGuestsPerRoom) {
    this.averageGuestsPerRoom = JsonNullable.of(averageGuestsPerRoom);
    return this;
  }

  /**
   * In-house guests / in-house rooms.
   * @return averageGuestsPerRoom
   */
  @NotNull 
  @JsonProperty("averageGuestsPerRoom")
  public JsonNullable<String> getAverageGuestsPerRoom() {
    return averageGuestsPerRoom;
  }

  public void setAverageGuestsPerRoom(JsonNullable<String> averageGuestsPerRoom) {
    this.averageGuestsPerRoom = averageGuestsPerRoom;
  }

  public ManagerGuestStatistic averageRatePerGuest(String averageRatePerGuest) {
    this.averageRatePerGuest = JsonNullable.of(averageRatePerGuest);
    return this;
  }

  /**
   * Room revenue of the in-house rooms / in-house guests.
   * @return averageRatePerGuest
   */
  @NotNull 
  @JsonProperty("averageRatePerGuest")
  public JsonNullable<String> getAverageRatePerGuest() {
    return averageRatePerGuest;
  }

  public void setAverageRatePerGuest(JsonNullable<String> averageRatePerGuest) {
    this.averageRatePerGuest = averageRatePerGuest;
  }

  public ManagerGuestStatistic averageLengthOfStay(String averageLengthOfStay) {
    this.averageLengthOfStay = JsonNullable.of(averageLengthOfStay);
    return this;
  }

  /**
   * Nights, averaged over the in-house bookings' whole stays.
   * @return averageLengthOfStay
   */
  @NotNull 
  @JsonProperty("averageLengthOfStay")
  public JsonNullable<String> getAverageLengthOfStay() {
    return averageLengthOfStay;
  }

  public void setAverageLengthOfStay(JsonNullable<String> averageLengthOfStay) {
    this.averageLengthOfStay = averageLengthOfStay;
  }

  public ManagerGuestStatistic complimentaryGuests(Integer complimentaryGuests) {
    this.complimentaryGuests = complimentaryGuests;
    return this;
  }

  /**
   * Get complimentaryGuests
   * @return complimentaryGuests
   */
  @NotNull 
  @JsonProperty("complimentaryGuests")
  public Integer getComplimentaryGuests() {
    return complimentaryGuests;
  }

  public void setComplimentaryGuests(Integer complimentaryGuests) {
    this.complimentaryGuests = complimentaryGuests;
  }

  public ManagerGuestStatistic houseUseGuests(Integer houseUseGuests) {
    this.houseUseGuests = houseUseGuests;
    return this;
  }

  /**
   * Get houseUseGuests
   * @return houseUseGuests
   */
  @NotNull 
  @JsonProperty("houseUseGuests")
  public Integer getHouseUseGuests() {
    return houseUseGuests;
  }

  public void setHouseUseGuests(Integer houseUseGuests) {
    this.houseUseGuests = houseUseGuests;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ManagerGuestStatistic managerGuestStatistic = (ManagerGuestStatistic) o;
    return Objects.equals(this.adultsInHouse, managerGuestStatistic.adultsInHouse) &&
        Objects.equals(this.childrenInHouse, managerGuestStatistic.childrenInHouse) &&
        Objects.equals(this.guestsInHouse, managerGuestStatistic.guestsInHouse) &&
        Objects.equals(this.averageGuestsPerRoom, managerGuestStatistic.averageGuestsPerRoom) &&
        Objects.equals(this.averageRatePerGuest, managerGuestStatistic.averageRatePerGuest) &&
        Objects.equals(this.averageLengthOfStay, managerGuestStatistic.averageLengthOfStay) &&
        Objects.equals(this.complimentaryGuests, managerGuestStatistic.complimentaryGuests) &&
        Objects.equals(this.houseUseGuests, managerGuestStatistic.houseUseGuests);
  }

  @Override
  public int hashCode() {
    return Objects.hash(adultsInHouse, childrenInHouse, guestsInHouse, averageGuestsPerRoom, averageRatePerGuest, averageLengthOfStay, complimentaryGuests, houseUseGuests);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ManagerGuestStatistic {\n");
    sb.append("    adultsInHouse: ").append(toIndentedString(adultsInHouse)).append("\n");
    sb.append("    childrenInHouse: ").append(toIndentedString(childrenInHouse)).append("\n");
    sb.append("    guestsInHouse: ").append(toIndentedString(guestsInHouse)).append("\n");
    sb.append("    averageGuestsPerRoom: ").append(toIndentedString(averageGuestsPerRoom)).append("\n");
    sb.append("    averageRatePerGuest: ").append(toIndentedString(averageRatePerGuest)).append("\n");
    sb.append("    averageLengthOfStay: ").append(toIndentedString(averageLengthOfStay)).append("\n");
    sb.append("    complimentaryGuests: ").append(toIndentedString(complimentaryGuests)).append("\n");
    sb.append("    houseUseGuests: ").append(toIndentedString(houseUseGuests)).append("\n");
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

