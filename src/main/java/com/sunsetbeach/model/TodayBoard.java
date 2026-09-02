package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.TodayBoardEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Response of &#x60;GET /bookings/today&#x60; - the front desk&#39;s daily working set, in the three groups reception actually thinks in. A booking appears in exactly one list: &#x60;arrivingToday&#x60; (&#x60;checkIn&#x60; &#x3D; today, &#x60;occupancyStatus&#x60; &#x3D; &#x60;EXPECTED&#x60;), &#x60;departingToday&#x60; (&#x60;checkOut&#x60; &#x3D; today, &#x60;occupancyStatus&#x60; &#x3D; &#x60;CHECKED_IN&#x60;), or &#x60;inHouse&#x60; (&#x60;occupancyStatus&#x60; &#x3D; &#x60;CHECKED_IN&#x60;, regardless of date - a guest mid-stay, not just one departing today; &#x60;departingToday&#x60; is a subset by date, not a separate population). A booking marked &#x60;NO_SHOW&#x60; or already &#x60;CHECKED_OUT&#x60; today appears in none of the three - it&#39;s no longer part of today&#39;s open work. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class TodayBoard {

  @Valid
  private List<@Valid TodayBoardEntry> arrivingToday = new ArrayList<>();

  @Valid
  private List<@Valid TodayBoardEntry> departingToday = new ArrayList<>();

  @Valid
  private List<@Valid TodayBoardEntry> inHouse = new ArrayList<>();

  public TodayBoard() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TodayBoard(List<@Valid TodayBoardEntry> arrivingToday, List<@Valid TodayBoardEntry> departingToday, List<@Valid TodayBoardEntry> inHouse) {
    this.arrivingToday = arrivingToday;
    this.departingToday = departingToday;
    this.inHouse = inHouse;
  }

  public TodayBoard arrivingToday(List<@Valid TodayBoardEntry> arrivingToday) {
    this.arrivingToday = arrivingToday;
    return this;
  }

  public TodayBoard addArrivingTodayItem(TodayBoardEntry arrivingTodayItem) {
    if (this.arrivingToday == null) {
      this.arrivingToday = new ArrayList<>();
    }
    this.arrivingToday.add(arrivingTodayItem);
    return this;
  }

  /**
   * Get arrivingToday
   * @return arrivingToday
   */
  @NotNull @Valid 
  @JsonProperty("arrivingToday")
  public List<@Valid TodayBoardEntry> getArrivingToday() {
    return arrivingToday;
  }

  public void setArrivingToday(List<@Valid TodayBoardEntry> arrivingToday) {
    this.arrivingToday = arrivingToday;
  }

  public TodayBoard departingToday(List<@Valid TodayBoardEntry> departingToday) {
    this.departingToday = departingToday;
    return this;
  }

  public TodayBoard addDepartingTodayItem(TodayBoardEntry departingTodayItem) {
    if (this.departingToday == null) {
      this.departingToday = new ArrayList<>();
    }
    this.departingToday.add(departingTodayItem);
    return this;
  }

  /**
   * Get departingToday
   * @return departingToday
   */
  @NotNull @Valid 
  @JsonProperty("departingToday")
  public List<@Valid TodayBoardEntry> getDepartingToday() {
    return departingToday;
  }

  public void setDepartingToday(List<@Valid TodayBoardEntry> departingToday) {
    this.departingToday = departingToday;
  }

  public TodayBoard inHouse(List<@Valid TodayBoardEntry> inHouse) {
    this.inHouse = inHouse;
    return this;
  }

  public TodayBoard addInHouseItem(TodayBoardEntry inHouseItem) {
    if (this.inHouse == null) {
      this.inHouse = new ArrayList<>();
    }
    this.inHouse.add(inHouseItem);
    return this;
  }

  /**
   * Get inHouse
   * @return inHouse
   */
  @NotNull @Valid 
  @JsonProperty("inHouse")
  public List<@Valid TodayBoardEntry> getInHouse() {
    return inHouse;
  }

  public void setInHouse(List<@Valid TodayBoardEntry> inHouse) {
    this.inHouse = inHouse;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TodayBoard todayBoard = (TodayBoard) o;
    return Objects.equals(this.arrivingToday, todayBoard.arrivingToday) &&
        Objects.equals(this.departingToday, todayBoard.departingToday) &&
        Objects.equals(this.inHouse, todayBoard.inHouse);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arrivingToday, departingToday, inHouse);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TodayBoard {\n");
    sb.append("    arrivingToday: ").append(toIndentedString(arrivingToday)).append("\n");
    sb.append("    departingToday: ").append(toIndentedString(departingToday)).append("\n");
    sb.append("    inHouse: ").append(toIndentedString(inHouse)).append("\n");
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

