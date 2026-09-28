package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Controls the daily sweep that sends automated lifecycle emails. The settings decide *whether* and *when* each type is sent; the wording is fixed in the backend. Who can receive one: only a guest whose &#x60;Guest&#x60; card is linked to a &#x60;GuestAccount&#x60; with a verified email that hasn&#39;t opted out (&#x60;GET /guest-auth/unsubscribe&#x60;). A &#x60;Guest&#x60; card without an account never gets one - staff typing a name at the front desk is not the guest&#39;s consent to automated mail. The email goes to the account&#39;s verified address. - Pre-arrival: bookings not &#x60;CANCELLED&#x60; whose &#x60;checkIn&#x60; is exactly &#x60;preArrivalDaysBefore&#x60;   days from today (hotel time), once per booking. - Post-stay: bookings not &#x60;CANCELLED&#x60; and not marked no-show whose &#x60;checkOut&#x60; was exactly   &#x60;postStayDaysAfter&#x60; days ago, once per booking. Without a &#x60;postStayReviewUrl&#x60; the email   goes out with no review section. - Win-back: a guest whose most recent non-&#x60;CANCELLED&#x60; booking checked out more than   &#x60;winBackMonthsSinceStay&#x60; months ago, and who hasn&#39;t had a win-back email within that   same many months.  Every type starts disabled. The sweep runs once a day; a day it misses (server down) is not caught up, since \&quot;exactly N days before\&quot; has passed for those bookings. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class LifecycleEmailSettings {

  private Boolean preArrivalEnabled;

  private Integer preArrivalDaysBefore;

  private Boolean postStayEnabled;

  private Integer postStayDaysAfter;

  private JsonNullable<String> postStayReviewUrl = JsonNullable.<String>undefined();

  private Boolean winBackEnabled;

  private Integer winBackMonthsSinceStay;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public LifecycleEmailSettings() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LifecycleEmailSettings(Boolean preArrivalEnabled, Integer preArrivalDaysBefore, Boolean postStayEnabled, Integer postStayDaysAfter, String postStayReviewUrl, Boolean winBackEnabled, Integer winBackMonthsSinceStay, OffsetDateTime updatedAt) {
    this.preArrivalEnabled = preArrivalEnabled;
    this.preArrivalDaysBefore = preArrivalDaysBefore;
    this.postStayEnabled = postStayEnabled;
    this.postStayDaysAfter = postStayDaysAfter;
    this.postStayReviewUrl = JsonNullable.of(postStayReviewUrl);
    this.winBackEnabled = winBackEnabled;
    this.winBackMonthsSinceStay = winBackMonthsSinceStay;
    this.updatedAt = updatedAt;
  }

  public LifecycleEmailSettings preArrivalEnabled(Boolean preArrivalEnabled) {
    this.preArrivalEnabled = preArrivalEnabled;
    return this;
  }

  /**
   * Get preArrivalEnabled
   * @return preArrivalEnabled
   */
  @NotNull 
  @JsonProperty("preArrivalEnabled")
  public Boolean getPreArrivalEnabled() {
    return preArrivalEnabled;
  }

  public void setPreArrivalEnabled(Boolean preArrivalEnabled) {
    this.preArrivalEnabled = preArrivalEnabled;
  }

  public LifecycleEmailSettings preArrivalDaysBefore(Integer preArrivalDaysBefore) {
    this.preArrivalDaysBefore = preArrivalDaysBefore;
    return this;
  }

  /**
   * Get preArrivalDaysBefore
   * @return preArrivalDaysBefore
   */
  @NotNull 
  @JsonProperty("preArrivalDaysBefore")
  public Integer getPreArrivalDaysBefore() {
    return preArrivalDaysBefore;
  }

  public void setPreArrivalDaysBefore(Integer preArrivalDaysBefore) {
    this.preArrivalDaysBefore = preArrivalDaysBefore;
  }

  public LifecycleEmailSettings postStayEnabled(Boolean postStayEnabled) {
    this.postStayEnabled = postStayEnabled;
    return this;
  }

  /**
   * Get postStayEnabled
   * @return postStayEnabled
   */
  @NotNull 
  @JsonProperty("postStayEnabled")
  public Boolean getPostStayEnabled() {
    return postStayEnabled;
  }

  public void setPostStayEnabled(Boolean postStayEnabled) {
    this.postStayEnabled = postStayEnabled;
  }

  public LifecycleEmailSettings postStayDaysAfter(Integer postStayDaysAfter) {
    this.postStayDaysAfter = postStayDaysAfter;
    return this;
  }

  /**
   * Get postStayDaysAfter
   * @return postStayDaysAfter
   */
  @NotNull 
  @JsonProperty("postStayDaysAfter")
  public Integer getPostStayDaysAfter() {
    return postStayDaysAfter;
  }

  public void setPostStayDaysAfter(Integer postStayDaysAfter) {
    this.postStayDaysAfter = postStayDaysAfter;
  }

  public LifecycleEmailSettings postStayReviewUrl(String postStayReviewUrl) {
    this.postStayReviewUrl = JsonNullable.of(postStayReviewUrl);
    return this;
  }

  /**
   * Get postStayReviewUrl
   * @return postStayReviewUrl
   */
  @NotNull 
  @JsonProperty("postStayReviewUrl")
  public JsonNullable<String> getPostStayReviewUrl() {
    return postStayReviewUrl;
  }

  public void setPostStayReviewUrl(JsonNullable<String> postStayReviewUrl) {
    this.postStayReviewUrl = postStayReviewUrl;
  }

  public LifecycleEmailSettings winBackEnabled(Boolean winBackEnabled) {
    this.winBackEnabled = winBackEnabled;
    return this;
  }

  /**
   * Get winBackEnabled
   * @return winBackEnabled
   */
  @NotNull 
  @JsonProperty("winBackEnabled")
  public Boolean getWinBackEnabled() {
    return winBackEnabled;
  }

  public void setWinBackEnabled(Boolean winBackEnabled) {
    this.winBackEnabled = winBackEnabled;
  }

  public LifecycleEmailSettings winBackMonthsSinceStay(Integer winBackMonthsSinceStay) {
    this.winBackMonthsSinceStay = winBackMonthsSinceStay;
    return this;
  }

  /**
   * Get winBackMonthsSinceStay
   * @return winBackMonthsSinceStay
   */
  @NotNull 
  @JsonProperty("winBackMonthsSinceStay")
  public Integer getWinBackMonthsSinceStay() {
    return winBackMonthsSinceStay;
  }

  public void setWinBackMonthsSinceStay(Integer winBackMonthsSinceStay) {
    this.winBackMonthsSinceStay = winBackMonthsSinceStay;
  }

  public LifecycleEmailSettings updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid 
  @JsonProperty("updatedAt")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LifecycleEmailSettings lifecycleEmailSettings = (LifecycleEmailSettings) o;
    return Objects.equals(this.preArrivalEnabled, lifecycleEmailSettings.preArrivalEnabled) &&
        Objects.equals(this.preArrivalDaysBefore, lifecycleEmailSettings.preArrivalDaysBefore) &&
        Objects.equals(this.postStayEnabled, lifecycleEmailSettings.postStayEnabled) &&
        Objects.equals(this.postStayDaysAfter, lifecycleEmailSettings.postStayDaysAfter) &&
        Objects.equals(this.postStayReviewUrl, lifecycleEmailSettings.postStayReviewUrl) &&
        Objects.equals(this.winBackEnabled, lifecycleEmailSettings.winBackEnabled) &&
        Objects.equals(this.winBackMonthsSinceStay, lifecycleEmailSettings.winBackMonthsSinceStay) &&
        Objects.equals(this.updatedAt, lifecycleEmailSettings.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(preArrivalEnabled, preArrivalDaysBefore, postStayEnabled, postStayDaysAfter, postStayReviewUrl, winBackEnabled, winBackMonthsSinceStay, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LifecycleEmailSettings {\n");
    sb.append("    preArrivalEnabled: ").append(toIndentedString(preArrivalEnabled)).append("\n");
    sb.append("    preArrivalDaysBefore: ").append(toIndentedString(preArrivalDaysBefore)).append("\n");
    sb.append("    postStayEnabled: ").append(toIndentedString(postStayEnabled)).append("\n");
    sb.append("    postStayDaysAfter: ").append(toIndentedString(postStayDaysAfter)).append("\n");
    sb.append("    postStayReviewUrl: ").append(toIndentedString(postStayReviewUrl)).append("\n");
    sb.append("    winBackEnabled: ").append(toIndentedString(winBackEnabled)).append("\n");
    sb.append("    winBackMonthsSinceStay: ").append(toIndentedString(winBackMonthsSinceStay)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

