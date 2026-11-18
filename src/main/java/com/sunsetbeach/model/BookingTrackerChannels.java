package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.BookingTrackerChannelRank;
import com.sunsetbeach.model.BookingTrackerChannelRow;
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
 * Every channel in the enum&#39;s order (zeros included), the total, and the channels with room nights ranked by net room revenue (ties: more room nights, then enum order). 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class BookingTrackerChannels {

  @Valid
  private List<@Valid BookingTrackerChannelRow> rows = new ArrayList<>();

  private BookingTrackerChannelRow total;

  @Valid
  private List<@Valid BookingTrackerChannelRank> ranking = new ArrayList<>();

  public BookingTrackerChannels() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BookingTrackerChannels(List<@Valid BookingTrackerChannelRow> rows, BookingTrackerChannelRow total, List<@Valid BookingTrackerChannelRank> ranking) {
    this.rows = rows;
    this.total = total;
    this.ranking = ranking;
  }

  public BookingTrackerChannels rows(List<@Valid BookingTrackerChannelRow> rows) {
    this.rows = rows;
    return this;
  }

  public BookingTrackerChannels addRowsItem(BookingTrackerChannelRow rowsItem) {
    if (this.rows == null) {
      this.rows = new ArrayList<>();
    }
    this.rows.add(rowsItem);
    return this;
  }

  /**
   * Get rows
   * @return rows
   */
  @NotNull @Valid 
  @JsonProperty("rows")
  public List<@Valid BookingTrackerChannelRow> getRows() {
    return rows;
  }

  public void setRows(List<@Valid BookingTrackerChannelRow> rows) {
    this.rows = rows;
  }

  public BookingTrackerChannels total(BookingTrackerChannelRow total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * @return total
   */
  @NotNull @Valid 
  @JsonProperty("total")
  public BookingTrackerChannelRow getTotal() {
    return total;
  }

  public void setTotal(BookingTrackerChannelRow total) {
    this.total = total;
  }

  public BookingTrackerChannels ranking(List<@Valid BookingTrackerChannelRank> ranking) {
    this.ranking = ranking;
    return this;
  }

  public BookingTrackerChannels addRankingItem(BookingTrackerChannelRank rankingItem) {
    if (this.ranking == null) {
      this.ranking = new ArrayList<>();
    }
    this.ranking.add(rankingItem);
    return this;
  }

  /**
   * Get ranking
   * @return ranking
   */
  @NotNull @Valid 
  @JsonProperty("ranking")
  public List<@Valid BookingTrackerChannelRank> getRanking() {
    return ranking;
  }

  public void setRanking(List<@Valid BookingTrackerChannelRank> ranking) {
    this.ranking = ranking;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookingTrackerChannels bookingTrackerChannels = (BookingTrackerChannels) o;
    return Objects.equals(this.rows, bookingTrackerChannels.rows) &&
        Objects.equals(this.total, bookingTrackerChannels.total) &&
        Objects.equals(this.ranking, bookingTrackerChannels.ranking);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rows, total, ranking);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookingTrackerChannels {\n");
    sb.append("    rows: ").append(toIndentedString(rows)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    ranking: ").append(toIndentedString(ranking)).append("\n");
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

