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
 * Body of &#x60;PUT /settings/lifecycle-emails&#x60; - every field, see &#x60;LifecycleEmailSettings&#x60;. &#x60;postStayReviewUrl&#x60; may be omitted or null for none. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class LifecycleEmailSettingsUpdateInput {

  private Boolean preArrivalEnabled;

  private Integer preArrivalDaysBefore;

  private Boolean postStayEnabled;

  private Integer postStayDaysAfter;

  private JsonNullable<@Size(max = 2000) String> postStayReviewUrl = JsonNullable.<String>undefined();

  private Boolean winBackEnabled;

  private Integer winBackMonthsSinceStay;

  public LifecycleEmailSettingsUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LifecycleEmailSettingsUpdateInput(Boolean preArrivalEnabled, Integer preArrivalDaysBefore, Boolean postStayEnabled, Integer postStayDaysAfter, Boolean winBackEnabled, Integer winBackMonthsSinceStay) {
    this.preArrivalEnabled = preArrivalEnabled;
    this.preArrivalDaysBefore = preArrivalDaysBefore;
    this.postStayEnabled = postStayEnabled;
    this.postStayDaysAfter = postStayDaysAfter;
    this.winBackEnabled = winBackEnabled;
    this.winBackMonthsSinceStay = winBackMonthsSinceStay;
  }

  public LifecycleEmailSettingsUpdateInput preArrivalEnabled(Boolean preArrivalEnabled) {
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

  public LifecycleEmailSettingsUpdateInput preArrivalDaysBefore(Integer preArrivalDaysBefore) {
    this.preArrivalDaysBefore = preArrivalDaysBefore;
    return this;
  }

  /**
   * Get preArrivalDaysBefore
   * minimum: 0
   * maximum: 60
   * @return preArrivalDaysBefore
   */
  @NotNull @Min(0) @Max(60) 
  @JsonProperty("preArrivalDaysBefore")
  public Integer getPreArrivalDaysBefore() {
    return preArrivalDaysBefore;
  }

  public void setPreArrivalDaysBefore(Integer preArrivalDaysBefore) {
    this.preArrivalDaysBefore = preArrivalDaysBefore;
  }

  public LifecycleEmailSettingsUpdateInput postStayEnabled(Boolean postStayEnabled) {
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

  public LifecycleEmailSettingsUpdateInput postStayDaysAfter(Integer postStayDaysAfter) {
    this.postStayDaysAfter = postStayDaysAfter;
    return this;
  }

  /**
   * Get postStayDaysAfter
   * minimum: 0
   * maximum: 60
   * @return postStayDaysAfter
   */
  @NotNull @Min(0) @Max(60) 
  @JsonProperty("postStayDaysAfter")
  public Integer getPostStayDaysAfter() {
    return postStayDaysAfter;
  }

  public void setPostStayDaysAfter(Integer postStayDaysAfter) {
    this.postStayDaysAfter = postStayDaysAfter;
  }

  public LifecycleEmailSettingsUpdateInput postStayReviewUrl(String postStayReviewUrl) {
    this.postStayReviewUrl = JsonNullable.of(postStayReviewUrl);
    return this;
  }

  /**
   * Get postStayReviewUrl
   * @return postStayReviewUrl
   */
  @Size(max = 2000) 
  @JsonProperty("postStayReviewUrl")
  public JsonNullable<@Size(max = 2000) String> getPostStayReviewUrl() {
    return postStayReviewUrl;
  }

  public void setPostStayReviewUrl(JsonNullable<String> postStayReviewUrl) {
    this.postStayReviewUrl = postStayReviewUrl;
  }

  public LifecycleEmailSettingsUpdateInput winBackEnabled(Boolean winBackEnabled) {
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

  public LifecycleEmailSettingsUpdateInput winBackMonthsSinceStay(Integer winBackMonthsSinceStay) {
    this.winBackMonthsSinceStay = winBackMonthsSinceStay;
    return this;
  }

  /**
   * Get winBackMonthsSinceStay
   * minimum: 1
   * maximum: 120
   * @return winBackMonthsSinceStay
   */
  @NotNull @Min(1) @Max(120) 
  @JsonProperty("winBackMonthsSinceStay")
  public Integer getWinBackMonthsSinceStay() {
    return winBackMonthsSinceStay;
  }

  public void setWinBackMonthsSinceStay(Integer winBackMonthsSinceStay) {
    this.winBackMonthsSinceStay = winBackMonthsSinceStay;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LifecycleEmailSettingsUpdateInput lifecycleEmailSettingsUpdateInput = (LifecycleEmailSettingsUpdateInput) o;
    return Objects.equals(this.preArrivalEnabled, lifecycleEmailSettingsUpdateInput.preArrivalEnabled) &&
        Objects.equals(this.preArrivalDaysBefore, lifecycleEmailSettingsUpdateInput.preArrivalDaysBefore) &&
        Objects.equals(this.postStayEnabled, lifecycleEmailSettingsUpdateInput.postStayEnabled) &&
        Objects.equals(this.postStayDaysAfter, lifecycleEmailSettingsUpdateInput.postStayDaysAfter) &&
        equalsNullable(this.postStayReviewUrl, lifecycleEmailSettingsUpdateInput.postStayReviewUrl) &&
        Objects.equals(this.winBackEnabled, lifecycleEmailSettingsUpdateInput.winBackEnabled) &&
        Objects.equals(this.winBackMonthsSinceStay, lifecycleEmailSettingsUpdateInput.winBackMonthsSinceStay);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(preArrivalEnabled, preArrivalDaysBefore, postStayEnabled, postStayDaysAfter, hashCodeNullable(postStayReviewUrl), winBackEnabled, winBackMonthsSinceStay);
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
    sb.append("class LifecycleEmailSettingsUpdateInput {\n");
    sb.append("    preArrivalEnabled: ").append(toIndentedString(preArrivalEnabled)).append("\n");
    sb.append("    preArrivalDaysBefore: ").append(toIndentedString(preArrivalDaysBefore)).append("\n");
    sb.append("    postStayEnabled: ").append(toIndentedString(postStayEnabled)).append("\n");
    sb.append("    postStayDaysAfter: ").append(toIndentedString(postStayDaysAfter)).append("\n");
    sb.append("    postStayReviewUrl: ").append(toIndentedString(postStayReviewUrl)).append("\n");
    sb.append("    winBackEnabled: ").append(toIndentedString(winBackEnabled)).append("\n");
    sb.append("    winBackMonthsSinceStay: ").append(toIndentedString(winBackMonthsSinceStay)).append("\n");
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

