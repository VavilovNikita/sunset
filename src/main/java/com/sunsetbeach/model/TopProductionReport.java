package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.TopProductionRow;
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
 * &#x60;GET /reports/top-production&#x60; - see that operation for what counts.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class TopProductionReport {

  private String from;

  private String to;

  @Valid
  private List<@Valid TopProductionRow> producers = new ArrayList<>();

  private TopProductionRow total;

  public TopProductionReport() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TopProductionReport(String from, String to, List<@Valid TopProductionRow> producers, TopProductionRow total) {
    this.from = from;
    this.to = to;
    this.producers = producers;
    this.total = total;
  }

  public TopProductionReport from(String from) {
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

  public TopProductionReport to(String to) {
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

  public TopProductionReport producers(List<@Valid TopProductionRow> producers) {
    this.producers = producers;
    return this;
  }

  public TopProductionReport addProducersItem(TopProductionRow producersItem) {
    if (this.producers == null) {
      this.producers = new ArrayList<>();
    }
    this.producers.add(producersItem);
    return this;
  }

  /**
   * One row per producer with at least one room-night in the range, most room-nights first.
   * @return producers
   */
  @NotNull @Valid 
  @JsonProperty("producers")
  public List<@Valid TopProductionRow> getProducers() {
    return producers;
  }

  public void setProducers(List<@Valid TopProductionRow> producers) {
    this.producers = producers;
  }

  public TopProductionReport total(TopProductionRow total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * @return total
   */
  @NotNull @Valid 
  @JsonProperty("total")
  public TopProductionRow getTotal() {
    return total;
  }

  public void setTotal(TopProductionRow total) {
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
    TopProductionReport topProductionReport = (TopProductionReport) o;
    return Objects.equals(this.from, topProductionReport.from) &&
        Objects.equals(this.to, topProductionReport.to) &&
        Objects.equals(this.producers, topProductionReport.producers) &&
        Objects.equals(this.total, topProductionReport.total);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, producers, total);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TopProductionReport {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    producers: ").append(toIndentedString(producers)).append("\n");
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

