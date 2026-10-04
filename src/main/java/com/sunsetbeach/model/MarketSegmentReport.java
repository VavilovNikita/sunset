package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.MarketSegmentRow;
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
 * &#x60;GET /reports/market-segment&#x60; - see that operation for what counts.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class MarketSegmentReport {

  private String from;

  private String to;

  @Valid
  private List<@Valid MarketSegmentRow> segments = new ArrayList<>();

  private MarketSegmentRow total;

  public MarketSegmentReport() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MarketSegmentReport(String from, String to, List<@Valid MarketSegmentRow> segments, MarketSegmentRow total) {
    this.from = from;
    this.to = to;
    this.segments = segments;
    this.total = total;
  }

  public MarketSegmentReport from(String from) {
    this.from = from;
    return this;
  }

  /**
   * Get from
   * @return from
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("from")
  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public MarketSegmentReport to(String to) {
    this.to = to;
    return this;
  }

  /**
   * Get to
   * @return to
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("to")
  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public MarketSegmentReport segments(List<@Valid MarketSegmentRow> segments) {
    this.segments = segments;
    return this;
  }

  public MarketSegmentReport addSegmentsItem(MarketSegmentRow segmentsItem) {
    if (this.segments == null) {
      this.segments = new ArrayList<>();
    }
    this.segments.add(segmentsItem);
    return this;
  }

  /**
   * Always all six segments, in the order COM, DIR, HFO, OTA, OTH, WLK.
   * @return segments
   */
  @NotNull @Valid 
  @JsonProperty("segments")
  public List<@Valid MarketSegmentRow> getSegments() {
    return segments;
  }

  public void setSegments(List<@Valid MarketSegmentRow> segments) {
    this.segments = segments;
  }

  public MarketSegmentReport total(MarketSegmentRow total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * @return total
   */
  @NotNull @Valid 
  @JsonProperty("total")
  public MarketSegmentRow getTotal() {
    return total;
  }

  public void setTotal(MarketSegmentRow total) {
    this.total = total;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MarketSegmentReport marketSegmentReport = (MarketSegmentReport) o;
    return Objects.equals(this.from, marketSegmentReport.from) &&
        Objects.equals(this.to, marketSegmentReport.to) &&
        Objects.equals(this.segments, marketSegmentReport.segments) &&
        Objects.equals(this.total, marketSegmentReport.total);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, segments, total);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MarketSegmentReport {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    segments: ").append(toIndentedString(segments)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
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

