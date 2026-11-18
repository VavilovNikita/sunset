package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.BookingTrackerChannels;
import com.sunsetbeach.model.BookingTrackerMonth;
import com.sunsetbeach.model.BookingTrackerSnapshot;
import com.sunsetbeach.model.BookingTrackerTrends;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * BookingTrackerReport
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingTrackerReport {

  private String fromMonth;

  private String toMonth;

  private JsonNullable<String> stayMonth = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate positionDate;

  private String vatRate;

  private String serviceChargeRate;

  @Valid
  private List<@Valid BookingTrackerMonth> months = new ArrayList<>();

  private BookingTrackerMonth total;

  private BookingTrackerChannels channels;

  private BookingTrackerTrends trends;

  @Valid
  private List<@Valid BookingTrackerSnapshot> weeklyReview = new ArrayList<>();

  public BookingTrackerReport() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingTrackerReport(String fromMonth, String toMonth, String stayMonth, LocalDate positionDate, String vatRate, String serviceChargeRate, List<@Valid BookingTrackerMonth> months, BookingTrackerMonth total, BookingTrackerChannels channels, BookingTrackerTrends trends, List<@Valid BookingTrackerSnapshot> weeklyReview) {
    this.fromMonth = fromMonth;
    this.toMonth = toMonth;
    this.stayMonth = JsonNullable.of(stayMonth);
    this.positionDate = positionDate;
    this.vatRate = vatRate;
    this.serviceChargeRate = serviceChargeRate;
    this.months = months;
    this.total = total;
    this.channels = channels;
    this.trends = trends;
    this.weeklyReview = weeklyReview;
  }

  public BookingTrackerReport fromMonth(String fromMonth) {
    this.fromMonth = fromMonth;
    return this;
  }

  /**
   * Get fromMonth
   * @return fromMonth
   */
  @NotNull 
  @JsonProperty("fromMonth")
  public String getFromMonth() {
    return fromMonth;
  }

  public void setFromMonth(String fromMonth) {
    this.fromMonth = fromMonth;
  }

  public BookingTrackerReport toMonth(String toMonth) {
    this.toMonth = toMonth;
    return this;
  }

  /**
   * Get toMonth
   * @return toMonth
   */
  @NotNull 
  @JsonProperty("toMonth")
  public String getToMonth() {
    return toMonth;
  }

  public void setToMonth(String toMonth) {
    this.toMonth = toMonth;
  }

  public BookingTrackerReport stayMonth(String stayMonth) {
    this.stayMonth = JsonNullable.of(stayMonth);
    return this;
  }

  /**
   * The filter that was applied to `channels` and `trends`; null for all stays.
   * @return stayMonth
   */
  @NotNull 
  @JsonProperty("stayMonth")
  public JsonNullable<String> getStayMonth() {
    return stayMonth;
  }

  public void setStayMonth(JsonNullable<String> stayMonth) {
    this.stayMonth = stayMonth;
  }

  public BookingTrackerReport positionDate(LocalDate positionDate) {
    this.positionDate = positionDate;
    return this;
  }

  /**
   * Today, hotel day.
   * @return positionDate
   */
  @NotNull @Valid 
  @JsonProperty("positionDate")
  public LocalDate getPositionDate() {
    return positionDate;
  }

  public void setPositionDate(LocalDate positionDate) {
    this.positionDate = positionDate;
  }

  public BookingTrackerReport vatRate(String vatRate) {
    this.vatRate = vatRate;
    return this;
  }

  /**
   * Get vatRate
   * @return vatRate
   */
  @NotNull 
  @JsonProperty("vatRate")
  public String getVatRate() {
    return vatRate;
  }

  public void setVatRate(String vatRate) {
    this.vatRate = vatRate;
  }

  public BookingTrackerReport serviceChargeRate(String serviceChargeRate) {
    this.serviceChargeRate = serviceChargeRate;
    return this;
  }

  /**
   * Get serviceChargeRate
   * @return serviceChargeRate
   */
  @NotNull 
  @JsonProperty("serviceChargeRate")
  public String getServiceChargeRate() {
    return serviceChargeRate;
  }

  public void setServiceChargeRate(String serviceChargeRate) {
    this.serviceChargeRate = serviceChargeRate;
  }

  public BookingTrackerReport months(List<@Valid BookingTrackerMonth> months) {
    this.months = months;
    return this;
  }

  public BookingTrackerReport addMonthsItem(BookingTrackerMonth monthsItem) {
    if (this.months == null) {
      this.months = new ArrayList<>();
    }
    this.months.add(monthsItem);
    return this;
  }

  /**
   * Get months
   * @return months
   */
  @NotNull @Valid 
  @JsonProperty("months")
  public List<@Valid BookingTrackerMonth> getMonths() {
    return months;
  }

  public void setMonths(List<@Valid BookingTrackerMonth> months) {
    this.months = months;
  }

  public BookingTrackerReport total(BookingTrackerMonth total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * @return total
   */
  @NotNull @Valid 
  @JsonProperty("total")
  public BookingTrackerMonth getTotal() {
    return total;
  }

  public void setTotal(BookingTrackerMonth total) {
    this.total = total;
  }

  public BookingTrackerReport channels(BookingTrackerChannels channels) {
    this.channels = channels;
    return this;
  }

  /**
   * Get channels
   * @return channels
   */
  @NotNull @Valid 
  @JsonProperty("channels")
  public BookingTrackerChannels getChannels() {
    return channels;
  }

  public void setChannels(BookingTrackerChannels channels) {
    this.channels = channels;
  }

  public BookingTrackerReport trends(BookingTrackerTrends trends) {
    this.trends = trends;
    return this;
  }

  /**
   * Get trends
   * @return trends
   */
  @NotNull @Valid 
  @JsonProperty("trends")
  public BookingTrackerTrends getTrends() {
    return trends;
  }

  public void setTrends(BookingTrackerTrends trends) {
    this.trends = trends;
  }

  public BookingTrackerReport weeklyReview(List<@Valid BookingTrackerSnapshot> weeklyReview) {
    this.weeklyReview = weeklyReview;
    return this;
  }

  public BookingTrackerReport addWeeklyReviewItem(BookingTrackerSnapshot weeklyReviewItem) {
    if (this.weeklyReview == null) {
      this.weeklyReview = new ArrayList<>();
    }
    this.weeklyReview.add(weeklyReviewItem);
    return this;
  }

  /**
   * Get weeklyReview
   * @return weeklyReview
   */
  @NotNull @Valid 
  @JsonProperty("weeklyReview")
  public List<@Valid BookingTrackerSnapshot> getWeeklyReview() {
    return weeklyReview;
  }

  public void setWeeklyReview(List<@Valid BookingTrackerSnapshot> weeklyReview) {
    this.weeklyReview = weeklyReview;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingTrackerReport bookingTrackerReport = (BookingTrackerReport) o;
    return Objects.equals(this.fromMonth, bookingTrackerReport.fromMonth) &&
        Objects.equals(this.toMonth, bookingTrackerReport.toMonth) &&
        Objects.equals(this.stayMonth, bookingTrackerReport.stayMonth) &&
        Objects.equals(this.positionDate, bookingTrackerReport.positionDate) &&
        Objects.equals(this.vatRate, bookingTrackerReport.vatRate) &&
        Objects.equals(this.serviceChargeRate, bookingTrackerReport.serviceChargeRate) &&
        Objects.equals(this.months, bookingTrackerReport.months) &&
        Objects.equals(this.total, bookingTrackerReport.total) &&
        Objects.equals(this.channels, bookingTrackerReport.channels) &&
        Objects.equals(this.trends, bookingTrackerReport.trends) &&
        Objects.equals(this.weeklyReview, bookingTrackerReport.weeklyReview);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromMonth, toMonth, stayMonth, positionDate, vatRate, serviceChargeRate, months, total, channels, trends, weeklyReview);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingTrackerReport {\n");
    sb.append("    fromMonth: ").append(toIndentedString(fromMonth)).append("\n");
    sb.append("    toMonth: ").append(toIndentedString(toMonth)).append("\n");
    sb.append("    stayMonth: ").append(toIndentedString(stayMonth)).append("\n");
    sb.append("    positionDate: ").append(toIndentedString(positionDate)).append("\n");
    sb.append("    vatRate: ").append(toIndentedString(vatRate)).append("\n");
    sb.append("    serviceChargeRate: ").append(toIndentedString(serviceChargeRate)).append("\n");
    sb.append("    months: ").append(toIndentedString(months)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    channels: ").append(toIndentedString(channels)).append("\n");
    sb.append("    trends: ").append(toIndentedString(trends)).append("\n");
    sb.append("    weeklyReview: ").append(toIndentedString(weeklyReview)).append("\n");
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

