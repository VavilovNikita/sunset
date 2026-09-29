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
 * ManagerRoomStatistic
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ManagerRoomStatistic {

  private Integer totalRooms;

  private Integer outOfOrder;

  private Integer availableForSale;

  private Integer occupied;

  private Integer complimentary;

  private Integer houseUse;

  private Integer occupiedExcludingCompAndHouseUse;

  private JsonNullable<String> occupancyPercent = JsonNullable.<String>undefined();

  private JsonNullable<String> averageRatePerOccupiedRoom = JsonNullable.<String>undefined();

  private JsonNullable<String> averageRevenuePerAvailableRoom = JsonNullable.<String>undefined();

  public ManagerRoomStatistic() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ManagerRoomStatistic(Integer totalRooms, Integer outOfOrder, Integer availableForSale, Integer occupied, Integer complimentary, Integer houseUse, Integer occupiedExcludingCompAndHouseUse, String occupancyPercent, String averageRatePerOccupiedRoom, String averageRevenuePerAvailableRoom) {
    this.totalRooms = totalRooms;
    this.outOfOrder = outOfOrder;
    this.availableForSale = availableForSale;
    this.occupied = occupied;
    this.complimentary = complimentary;
    this.houseUse = houseUse;
    this.occupiedExcludingCompAndHouseUse = occupiedExcludingCompAndHouseUse;
    this.occupancyPercent = JsonNullable.of(occupancyPercent);
    this.averageRatePerOccupiedRoom = JsonNullable.of(averageRatePerOccupiedRoom);
    this.averageRevenuePerAvailableRoom = JsonNullable.of(averageRevenuePerAvailableRoom);
  }

  public ManagerRoomStatistic totalRooms(Integer totalRooms) {
    this.totalRooms = totalRooms;
    return this;
  }

  /**
   * Get totalRooms
   * @return totalRooms
   */
  @NotNull 
  @JsonProperty("totalRooms")
  public Integer getTotalRooms() {
    return totalRooms;
  }

  public void setTotalRooms(Integer totalRooms) {
    this.totalRooms = totalRooms;
  }

  public ManagerRoomStatistic outOfOrder(Integer outOfOrder) {
    this.outOfOrder = outOfOrder;
    return this;
  }

  /**
   * Get outOfOrder
   * @return outOfOrder
   */
  @NotNull 
  @JsonProperty("outOfOrder")
  public Integer getOutOfOrder() {
    return outOfOrder;
  }

  public void setOutOfOrder(Integer outOfOrder) {
    this.outOfOrder = outOfOrder;
  }

  public ManagerRoomStatistic availableForSale(Integer availableForSale) {
    this.availableForSale = availableForSale;
    return this;
  }

  /**
   * Get availableForSale
   * @return availableForSale
   */
  @NotNull 
  @JsonProperty("availableForSale")
  public Integer getAvailableForSale() {
    return availableForSale;
  }

  public void setAvailableForSale(Integer availableForSale) {
    this.availableForSale = availableForSale;
  }

  public ManagerRoomStatistic occupied(Integer occupied) {
    this.occupied = occupied;
    return this;
  }

  /**
   * Get occupied
   * @return occupied
   */
  @NotNull 
  @JsonProperty("occupied")
  public Integer getOccupied() {
    return occupied;
  }

  public void setOccupied(Integer occupied) {
    this.occupied = occupied;
  }

  public ManagerRoomStatistic complimentary(Integer complimentary) {
    this.complimentary = complimentary;
    return this;
  }

  /**
   * Get complimentary
   * @return complimentary
   */
  @NotNull 
  @JsonProperty("complimentary")
  public Integer getComplimentary() {
    return complimentary;
  }

  public void setComplimentary(Integer complimentary) {
    this.complimentary = complimentary;
  }

  public ManagerRoomStatistic houseUse(Integer houseUse) {
    this.houseUse = houseUse;
    return this;
  }

  /**
   * Get houseUse
   * @return houseUse
   */
  @NotNull 
  @JsonProperty("houseUse")
  public Integer getHouseUse() {
    return houseUse;
  }

  public void setHouseUse(Integer houseUse) {
    this.houseUse = houseUse;
  }

  public ManagerRoomStatistic occupiedExcludingCompAndHouseUse(Integer occupiedExcludingCompAndHouseUse) {
    this.occupiedExcludingCompAndHouseUse = occupiedExcludingCompAndHouseUse;
    return this;
  }

  /**
   * Get occupiedExcludingCompAndHouseUse
   * @return occupiedExcludingCompAndHouseUse
   */
  @NotNull 
  @JsonProperty("occupiedExcludingCompAndHouseUse")
  public Integer getOccupiedExcludingCompAndHouseUse() {
    return occupiedExcludingCompAndHouseUse;
  }

  public void setOccupiedExcludingCompAndHouseUse(Integer occupiedExcludingCompAndHouseUse) {
    this.occupiedExcludingCompAndHouseUse = occupiedExcludingCompAndHouseUse;
  }

  public ManagerRoomStatistic occupancyPercent(String occupancyPercent) {
    this.occupancyPercent = JsonNullable.of(occupancyPercent);
    return this;
  }

  /**
   * occupied / availableForSale × 100.
   * @return occupancyPercent
   */
  @NotNull 
  @JsonProperty("occupancyPercent")
  public JsonNullable<String> getOccupancyPercent() {
    return occupancyPercent;
  }

  public void setOccupancyPercent(JsonNullable<String> occupancyPercent) {
    this.occupancyPercent = occupancyPercent;
  }

  public ManagerRoomStatistic averageRatePerOccupiedRoom(String averageRatePerOccupiedRoom) {
    this.averageRatePerOccupiedRoom = JsonNullable.of(averageRatePerOccupiedRoom);
    return this;
  }

  /**
   * Room revenue / occupied (ADR-like).
   * @return averageRatePerOccupiedRoom
   */
  @NotNull 
  @JsonProperty("averageRatePerOccupiedRoom")
  public JsonNullable<String> getAverageRatePerOccupiedRoom() {
    return averageRatePerOccupiedRoom;
  }

  public void setAverageRatePerOccupiedRoom(JsonNullable<String> averageRatePerOccupiedRoom) {
    this.averageRatePerOccupiedRoom = averageRatePerOccupiedRoom;
  }

  public ManagerRoomStatistic averageRevenuePerAvailableRoom(String averageRevenuePerAvailableRoom) {
    this.averageRevenuePerAvailableRoom = JsonNullable.of(averageRevenuePerAvailableRoom);
    return this;
  }

  /**
   * Room revenue / availableForSale (RevPAR-like).
   * @return averageRevenuePerAvailableRoom
   */
  @NotNull 
  @JsonProperty("averageRevenuePerAvailableRoom")
  public JsonNullable<String> getAverageRevenuePerAvailableRoom() {
    return averageRevenuePerAvailableRoom;
  }

  public void setAverageRevenuePerAvailableRoom(JsonNullable<String> averageRevenuePerAvailableRoom) {
    this.averageRevenuePerAvailableRoom = averageRevenuePerAvailableRoom;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ManagerRoomStatistic managerRoomStatistic = (ManagerRoomStatistic) o;
    return Objects.equals(this.totalRooms, managerRoomStatistic.totalRooms) &&
        Objects.equals(this.outOfOrder, managerRoomStatistic.outOfOrder) &&
        Objects.equals(this.availableForSale, managerRoomStatistic.availableForSale) &&
        Objects.equals(this.occupied, managerRoomStatistic.occupied) &&
        Objects.equals(this.complimentary, managerRoomStatistic.complimentary) &&
        Objects.equals(this.houseUse, managerRoomStatistic.houseUse) &&
        Objects.equals(this.occupiedExcludingCompAndHouseUse, managerRoomStatistic.occupiedExcludingCompAndHouseUse) &&
        Objects.equals(this.occupancyPercent, managerRoomStatistic.occupancyPercent) &&
        Objects.equals(this.averageRatePerOccupiedRoom, managerRoomStatistic.averageRatePerOccupiedRoom) &&
        Objects.equals(this.averageRevenuePerAvailableRoom, managerRoomStatistic.averageRevenuePerAvailableRoom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalRooms, outOfOrder, availableForSale, occupied, complimentary, houseUse, occupiedExcludingCompAndHouseUse, occupancyPercent, averageRatePerOccupiedRoom, averageRevenuePerAvailableRoom);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ManagerRoomStatistic {\n");
    sb.append("    totalRooms: ").append(toIndentedString(totalRooms)).append("\n");
    sb.append("    outOfOrder: ").append(toIndentedString(outOfOrder)).append("\n");
    sb.append("    availableForSale: ").append(toIndentedString(availableForSale)).append("\n");
    sb.append("    occupied: ").append(toIndentedString(occupied)).append("\n");
    sb.append("    complimentary: ").append(toIndentedString(complimentary)).append("\n");
    sb.append("    houseUse: ").append(toIndentedString(houseUse)).append("\n");
    sb.append("    occupiedExcludingCompAndHouseUse: ").append(toIndentedString(occupiedExcludingCompAndHouseUse)).append("\n");
    sb.append("    occupancyPercent: ").append(toIndentedString(occupancyPercent)).append("\n");
    sb.append("    averageRatePerOccupiedRoom: ").append(toIndentedString(averageRatePerOccupiedRoom)).append("\n");
    sb.append("    averageRevenuePerAvailableRoom: ").append(toIndentedString(averageRevenuePerAvailableRoom)).append("\n");
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

