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
 * One stay month of the booking tracker (and, with &#x60;month&#x60; null, the total row). See &#x60;GET /reports/booking-tracker&#x60; for every definition. Money is a decimal string, percentages are percent with two decimals, a figure that cannot be computed is &#x60;null&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingTrackerMonth {

  private JsonNullable<String> month = JsonNullable.<String>undefined();

  private JsonNullable<Integer> physicalRooms = JsonNullable.<Integer>undefined();

  private Integer nightsInMonth;

  private Integer blockedRoomNights;

  private Integer availableRoomNights;

  private Integer bookedRoomNights;

  private JsonNullable<String> occupancyPercent = JsonNullable.<String>undefined();

  private String grossRevenue;

  private String packageRevenue;

  private String serviceCharge;

  private String vat;

  private String breakfastRevenue;

  private String netRoomRevenue;

  private JsonNullable<String> netAdr = JsonNullable.<String>undefined();

  private JsonNullable<Integer> baselineRoomNights = JsonNullable.<Integer>undefined();

  private JsonNullable<String> baselineNetRevenue = JsonNullable.<String>undefined();

  private JsonNullable<Integer> targetRoomNights = JsonNullable.<Integer>undefined();

  private JsonNullable<Integer> stillNeededRoomNights = JsonNullable.<Integer>undefined();

  private JsonNullable<Integer> pickupVsBaseline = JsonNullable.<Integer>undefined();

  public BookingTrackerMonth() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingTrackerMonth(String month, Integer physicalRooms, Integer nightsInMonth, Integer blockedRoomNights, Integer availableRoomNights, Integer bookedRoomNights, String occupancyPercent, String grossRevenue, String packageRevenue, String serviceCharge, String vat, String breakfastRevenue, String netRoomRevenue, String netAdr, Integer baselineRoomNights, String baselineNetRevenue, Integer targetRoomNights, Integer stillNeededRoomNights, Integer pickupVsBaseline) {
    this.month = JsonNullable.of(month);
    this.physicalRooms = JsonNullable.of(physicalRooms);
    this.nightsInMonth = nightsInMonth;
    this.blockedRoomNights = blockedRoomNights;
    this.availableRoomNights = availableRoomNights;
    this.bookedRoomNights = bookedRoomNights;
    this.occupancyPercent = JsonNullable.of(occupancyPercent);
    this.grossRevenue = grossRevenue;
    this.packageRevenue = packageRevenue;
    this.serviceCharge = serviceCharge;
    this.vat = vat;
    this.breakfastRevenue = breakfastRevenue;
    this.netRoomRevenue = netRoomRevenue;
    this.netAdr = JsonNullable.of(netAdr);
    this.baselineRoomNights = JsonNullable.of(baselineRoomNights);
    this.baselineNetRevenue = JsonNullable.of(baselineNetRevenue);
    this.targetRoomNights = JsonNullable.of(targetRoomNights);
    this.stillNeededRoomNights = JsonNullable.of(stillNeededRoomNights);
    this.pickupVsBaseline = JsonNullable.of(pickupVsBaseline);
  }

  public BookingTrackerMonth month(String month) {
    this.month = JsonNullable.of(month);
    return this;
  }

  /**
   * `YYYY-MM`; null on the total row.
   * @return month
   */
  @NotNull 
  @JsonProperty("month")
  public JsonNullable<String> getMonth() {
    return month;
  }

  public void setMonth(JsonNullable<String> month) {
    this.month = month;
  }

  public BookingTrackerMonth physicalRooms(Integer physicalRooms) {
    this.physicalRooms = JsonNullable.of(physicalRooms);
    return this;
  }

  /**
   * Units active today; null on the total row.
   * @return physicalRooms
   */
  @NotNull 
  @JsonProperty("physicalRooms")
  public JsonNullable<Integer> getPhysicalRooms() {
    return physicalRooms;
  }

  public void setPhysicalRooms(JsonNullable<Integer> physicalRooms) {
    this.physicalRooms = physicalRooms;
  }

  public BookingTrackerMonth nightsInMonth(Integer nightsInMonth) {
    this.nightsInMonth = nightsInMonth;
    return this;
  }

  /**
   * Get nightsInMonth
   * @return nightsInMonth
   */
  @NotNull 
  @JsonProperty("nightsInMonth")
  public Integer getNightsInMonth() {
    return nightsInMonth;
  }

  public void setNightsInMonth(Integer nightsInMonth) {
    this.nightsInMonth = nightsInMonth;
  }

  public BookingTrackerMonth blockedRoomNights(Integer blockedRoomNights) {
    this.blockedRoomNights = blockedRoomNights;
    return this;
  }

  /**
   * Get blockedRoomNights
   * @return blockedRoomNights
   */
  @NotNull 
  @JsonProperty("blockedRoomNights")
  public Integer getBlockedRoomNights() {
    return blockedRoomNights;
  }

  public void setBlockedRoomNights(Integer blockedRoomNights) {
    this.blockedRoomNights = blockedRoomNights;
  }

  public BookingTrackerMonth availableRoomNights(Integer availableRoomNights) {
    this.availableRoomNights = availableRoomNights;
    return this;
  }

  /**
   * Get availableRoomNights
   * @return availableRoomNights
   */
  @NotNull 
  @JsonProperty("availableRoomNights")
  public Integer getAvailableRoomNights() {
    return availableRoomNights;
  }

  public void setAvailableRoomNights(Integer availableRoomNights) {
    this.availableRoomNights = availableRoomNights;
  }

  public BookingTrackerMonth bookedRoomNights(Integer bookedRoomNights) {
    this.bookedRoomNights = bookedRoomNights;
    return this;
  }

  /**
   * Get bookedRoomNights
   * @return bookedRoomNights
   */
  @NotNull 
  @JsonProperty("bookedRoomNights")
  public Integer getBookedRoomNights() {
    return bookedRoomNights;
  }

  public void setBookedRoomNights(Integer bookedRoomNights) {
    this.bookedRoomNights = bookedRoomNights;
  }

  public BookingTrackerMonth occupancyPercent(String occupancyPercent) {
    this.occupancyPercent = JsonNullable.of(occupancyPercent);
    return this;
  }

  /**
   * Booked / available room nights; null when nothing is available.
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

  public BookingTrackerMonth grossRevenue(String grossRevenue) {
    this.grossRevenue = grossRevenue;
    return this;
  }

  /**
   * The inclusive package - the agreed price with VAT and service charge inside.
   * @return grossRevenue
   */
  @NotNull 
  @JsonProperty("grossRevenue")
  public String getGrossRevenue() {
    return grossRevenue;
  }

  public void setGrossRevenue(String grossRevenue) {
    this.grossRevenue = grossRevenue;
  }

  public BookingTrackerMonth packageRevenue(String packageRevenue) {
    this.packageRevenue = packageRevenue;
    return this;
  }

  /**
   * Gross / (1 + service charge) / (1 + VAT), before breakfast.
   * @return packageRevenue
   */
  @NotNull 
  @JsonProperty("packageRevenue")
  public String getPackageRevenue() {
    return packageRevenue;
  }

  public void setPackageRevenue(String packageRevenue) {
    this.packageRevenue = packageRevenue;
  }

  public BookingTrackerMonth serviceCharge(String serviceCharge) {
    this.serviceCharge = serviceCharge;
    return this;
  }

  /**
   * Get serviceCharge
   * @return serviceCharge
   */
  @NotNull 
  @JsonProperty("serviceCharge")
  public String getServiceCharge() {
    return serviceCharge;
  }

  public void setServiceCharge(String serviceCharge) {
    this.serviceCharge = serviceCharge;
  }

  public BookingTrackerMonth vat(String vat) {
    this.vat = vat;
    return this;
  }

  /**
   * Get vat
   * @return vat
   */
  @NotNull 
  @JsonProperty("vat")
  public String getVat() {
    return vat;
  }

  public void setVat(String vat) {
    this.vat = vat;
  }

  public BookingTrackerMonth breakfastRevenue(String breakfastRevenue) {
    this.breakfastRevenue = breakfastRevenue;
    return this;
  }

  /**
   * Get breakfastRevenue
   * @return breakfastRevenue
   */
  @NotNull 
  @JsonProperty("breakfastRevenue")
  public String getBreakfastRevenue() {
    return breakfastRevenue;
  }

  public void setBreakfastRevenue(String breakfastRevenue) {
    this.breakfastRevenue = breakfastRevenue;
  }

  public BookingTrackerMonth netRoomRevenue(String netRoomRevenue) {
    this.netRoomRevenue = netRoomRevenue;
    return this;
  }

  /**
   * Package less breakfast.
   * @return netRoomRevenue
   */
  @NotNull 
  @JsonProperty("netRoomRevenue")
  public String getNetRoomRevenue() {
    return netRoomRevenue;
  }

  public void setNetRoomRevenue(String netRoomRevenue) {
    this.netRoomRevenue = netRoomRevenue;
  }

  public BookingTrackerMonth netAdr(String netAdr) {
    this.netAdr = JsonNullable.of(netAdr);
    return this;
  }

  /**
   * Net room revenue / booked room nights; null without room nights.
   * @return netAdr
   */
  @NotNull 
  @JsonProperty("netAdr")
  public JsonNullable<String> getNetAdr() {
    return netAdr;
  }

  public void setNetAdr(JsonNullable<String> netAdr) {
    this.netAdr = netAdr;
  }

  public BookingTrackerMonth baselineRoomNights(Integer baselineRoomNights) {
    this.baselineRoomNights = JsonNullable.of(baselineRoomNights);
    return this;
  }

  /**
   * Get baselineRoomNights
   * @return baselineRoomNights
   */
  @NotNull 
  @JsonProperty("baselineRoomNights")
  public JsonNullable<Integer> getBaselineRoomNights() {
    return baselineRoomNights;
  }

  public void setBaselineRoomNights(JsonNullable<Integer> baselineRoomNights) {
    this.baselineRoomNights = baselineRoomNights;
  }

  public BookingTrackerMonth baselineNetRevenue(String baselineNetRevenue) {
    this.baselineNetRevenue = JsonNullable.of(baselineNetRevenue);
    return this;
  }

  /**
   * Get baselineNetRevenue
   * @return baselineNetRevenue
   */
  @NotNull 
  @JsonProperty("baselineNetRevenue")
  public JsonNullable<String> getBaselineNetRevenue() {
    return baselineNetRevenue;
  }

  public void setBaselineNetRevenue(JsonNullable<String> baselineNetRevenue) {
    this.baselineNetRevenue = baselineNetRevenue;
  }

  public BookingTrackerMonth targetRoomNights(Integer targetRoomNights) {
    this.targetRoomNights = JsonNullable.of(targetRoomNights);
    return this;
  }

  /**
   * Get targetRoomNights
   * @return targetRoomNights
   */
  @NotNull 
  @JsonProperty("targetRoomNights")
  public JsonNullable<Integer> getTargetRoomNights() {
    return targetRoomNights;
  }

  public void setTargetRoomNights(JsonNullable<Integer> targetRoomNights) {
    this.targetRoomNights = targetRoomNights;
  }

  public BookingTrackerMonth stillNeededRoomNights(Integer stillNeededRoomNights) {
    this.stillNeededRoomNights = JsonNullable.of(stillNeededRoomNights);
    return this;
  }

  /**
   * max(0, target - booked); null without a target.
   * @return stillNeededRoomNights
   */
  @NotNull 
  @JsonProperty("stillNeededRoomNights")
  public JsonNullable<Integer> getStillNeededRoomNights() {
    return stillNeededRoomNights;
  }

  public void setStillNeededRoomNights(JsonNullable<Integer> stillNeededRoomNights) {
    this.stillNeededRoomNights = stillNeededRoomNights;
  }

  public BookingTrackerMonth pickupVsBaseline(Integer pickupVsBaseline) {
    this.pickupVsBaseline = JsonNullable.of(pickupVsBaseline);
    return this;
  }

  /**
   * Booked - baseline room nights; null without a baseline.
   * @return pickupVsBaseline
   */
  @NotNull 
  @JsonProperty("pickupVsBaseline")
  public JsonNullable<Integer> getPickupVsBaseline() {
    return pickupVsBaseline;
  }

  public void setPickupVsBaseline(JsonNullable<Integer> pickupVsBaseline) {
    this.pickupVsBaseline = pickupVsBaseline;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingTrackerMonth bookingTrackerMonth = (BookingTrackerMonth) o;
    return Objects.equals(this.month, bookingTrackerMonth.month) &&
        Objects.equals(this.physicalRooms, bookingTrackerMonth.physicalRooms) &&
        Objects.equals(this.nightsInMonth, bookingTrackerMonth.nightsInMonth) &&
        Objects.equals(this.blockedRoomNights, bookingTrackerMonth.blockedRoomNights) &&
        Objects.equals(this.availableRoomNights, bookingTrackerMonth.availableRoomNights) &&
        Objects.equals(this.bookedRoomNights, bookingTrackerMonth.bookedRoomNights) &&
        Objects.equals(this.occupancyPercent, bookingTrackerMonth.occupancyPercent) &&
        Objects.equals(this.grossRevenue, bookingTrackerMonth.grossRevenue) &&
        Objects.equals(this.packageRevenue, bookingTrackerMonth.packageRevenue) &&
        Objects.equals(this.serviceCharge, bookingTrackerMonth.serviceCharge) &&
        Objects.equals(this.vat, bookingTrackerMonth.vat) &&
        Objects.equals(this.breakfastRevenue, bookingTrackerMonth.breakfastRevenue) &&
        Objects.equals(this.netRoomRevenue, bookingTrackerMonth.netRoomRevenue) &&
        Objects.equals(this.netAdr, bookingTrackerMonth.netAdr) &&
        Objects.equals(this.baselineRoomNights, bookingTrackerMonth.baselineRoomNights) &&
        Objects.equals(this.baselineNetRevenue, bookingTrackerMonth.baselineNetRevenue) &&
        Objects.equals(this.targetRoomNights, bookingTrackerMonth.targetRoomNights) &&
        Objects.equals(this.stillNeededRoomNights, bookingTrackerMonth.stillNeededRoomNights) &&
        Objects.equals(this.pickupVsBaseline, bookingTrackerMonth.pickupVsBaseline);
  }

  @Override
  public int hashCode() {
    return Objects.hash(month, physicalRooms, nightsInMonth, blockedRoomNights, availableRoomNights, bookedRoomNights, occupancyPercent, grossRevenue, packageRevenue, serviceCharge, vat, breakfastRevenue, netRoomRevenue, netAdr, baselineRoomNights, baselineNetRevenue, targetRoomNights, stillNeededRoomNights, pickupVsBaseline);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingTrackerMonth {\n");
    sb.append("    month: ").append(toIndentedString(month)).append("\n");
    sb.append("    physicalRooms: ").append(toIndentedString(physicalRooms)).append("\n");
    sb.append("    nightsInMonth: ").append(toIndentedString(nightsInMonth)).append("\n");
    sb.append("    blockedRoomNights: ").append(toIndentedString(blockedRoomNights)).append("\n");
    sb.append("    availableRoomNights: ").append(toIndentedString(availableRoomNights)).append("\n");
    sb.append("    bookedRoomNights: ").append(toIndentedString(bookedRoomNights)).append("\n");
    sb.append("    occupancyPercent: ").append(toIndentedString(occupancyPercent)).append("\n");
    sb.append("    grossRevenue: ").append(toIndentedString(grossRevenue)).append("\n");
    sb.append("    packageRevenue: ").append(toIndentedString(packageRevenue)).append("\n");
    sb.append("    serviceCharge: ").append(toIndentedString(serviceCharge)).append("\n");
    sb.append("    vat: ").append(toIndentedString(vat)).append("\n");
    sb.append("    breakfastRevenue: ").append(toIndentedString(breakfastRevenue)).append("\n");
    sb.append("    netRoomRevenue: ").append(toIndentedString(netRoomRevenue)).append("\n");
    sb.append("    netAdr: ").append(toIndentedString(netAdr)).append("\n");
    sb.append("    baselineRoomNights: ").append(toIndentedString(baselineRoomNights)).append("\n");
    sb.append("    baselineNetRevenue: ").append(toIndentedString(baselineNetRevenue)).append("\n");
    sb.append("    targetRoomNights: ").append(toIndentedString(targetRoomNights)).append("\n");
    sb.append("    stillNeededRoomNights: ").append(toIndentedString(stillNeededRoomNights)).append("\n");
    sb.append("    pickupVsBaseline: ").append(toIndentedString(pickupVsBaseline)).append("\n");
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

