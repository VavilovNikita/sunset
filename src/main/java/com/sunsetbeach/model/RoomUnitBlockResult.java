package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sunsetbeach.model.RoomUnitBlock;
import com.sunsetbeach.model.RoomUnitBlockAffectedBooking;
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
 * Response of &#x60;POST /room-units/{id}/blocks&#x60;. &#x60;warning&#x60; is set (but the block is still created) when the blocked range overlaps one or more non-CANCELLED bookings - staff are told, not blocked, the same warn-don&#39;t-block shape as &#x60;CheckInResult&#x60;. Two distinct populations, because the manager&#39;s next move differs for each: &#x60;affectedBookings&#x60; are assigned to this exact physical unit (a certain conflict - the guest is in this specific room); &#x60;affectedUnassignedBookings&#x60; are booked into this unit&#39;s *room type* with no specific unit chosen yet (&#x60;Booking.roomUnitId&#x60; null) - not yet in this room, but pulling one more unit out of the type&#39;s pool can leave too few units for them, the same oversell &#x60;AvailabilityDay.availableCount&#x60; already models by not clamping at zero (see that schema). Both use the same overlap rule the calendar/availability engine already use to compare a booking against a block: &#x60;booking.checkIn &lt;&#x3D; block.toDate &amp;&amp; booking.checkOut &gt; block.fromDate&#x60; - &#x60;RoomUnitBlock.fromDate&#x60;/&#x60;toDate&#x60; are both inclusive, a booking&#39;s &#x60;checkOut&#x60; is exclusive (the departure day is free), so a block starting on a booking&#39;s checkout day does not warn. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class RoomUnitBlockResult {

  private RoomUnitBlock block;

  private JsonNullable<String> warning = JsonNullable.<String>undefined();

  @Valid
  private List<@Valid RoomUnitBlockAffectedBooking> affectedBookings = new ArrayList<>();

  @Valid
  private List<@Valid RoomUnitBlockAffectedBooking> affectedUnassignedBookings = new ArrayList<>();

  public RoomUnitBlockResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoomUnitBlockResult(RoomUnitBlock block, String warning, List<@Valid RoomUnitBlockAffectedBooking> affectedBookings, List<@Valid RoomUnitBlockAffectedBooking> affectedUnassignedBookings) {
    this.block = block;
    this.warning = JsonNullable.of(warning);
    this.affectedBookings = affectedBookings;
    this.affectedUnassignedBookings = affectedUnassignedBookings;
  }

  public RoomUnitBlockResult block(RoomUnitBlock block) {
    this.block = block;
    return this;
  }

  /**
   * Get block
   * @return block
   */
  @NotNull @Valid 
  @JsonProperty("block")
  public RoomUnitBlock getBlock() {
    return block;
  }

  public void setBlock(RoomUnitBlock block) {
    this.block = block;
  }

  public RoomUnitBlockResult warning(String warning) {
    this.warning = JsonNullable.of(warning);
    return this;
  }

  /**
   * Get warning
   * @return warning
   */
  @NotNull 
  @JsonProperty("warning")
  public JsonNullable<String> getWarning() {
    return warning;
  }

  public void setWarning(JsonNullable<String> warning) {
    this.warning = warning;
  }

  public RoomUnitBlockResult affectedBookings(List<@Valid RoomUnitBlockAffectedBooking> affectedBookings) {
    this.affectedBookings = affectedBookings;
    return this;
  }

  public RoomUnitBlockResult addAffectedBookingsItem(RoomUnitBlockAffectedBooking affectedBookingsItem) {
    if (this.affectedBookings == null) {
      this.affectedBookings = new ArrayList<>();
    }
    this.affectedBookings.add(affectedBookingsItem);
    return this;
  }

  /**
   * Get affectedBookings
   * @return affectedBookings
   */
  @NotNull @Valid 
  @JsonProperty("affectedBookings")
  public List<@Valid RoomUnitBlockAffectedBooking> getAffectedBookings() {
    return affectedBookings;
  }

  public void setAffectedBookings(List<@Valid RoomUnitBlockAffectedBooking> affectedBookings) {
    this.affectedBookings = affectedBookings;
  }

  public RoomUnitBlockResult affectedUnassignedBookings(List<@Valid RoomUnitBlockAffectedBooking> affectedUnassignedBookings) {
    this.affectedUnassignedBookings = affectedUnassignedBookings;
    return this;
  }

  public RoomUnitBlockResult addAffectedUnassignedBookingsItem(RoomUnitBlockAffectedBooking affectedUnassignedBookingsItem) {
    if (this.affectedUnassignedBookings == null) {
      this.affectedUnassignedBookings = new ArrayList<>();
    }
    this.affectedUnassignedBookings.add(affectedUnassignedBookingsItem);
    return this;
  }

  /**
   * Non-CANCELLED bookings of this unit's room type, over the same dates, with no room unit assigned yet - not tied to this specific room, but competing for the same pool of units it belongs to. Does not attempt to compute whether the type actually goes into oversell (that depends on every other unit/booking of the type over the whole range, not just this one block) - it names the population that could be affected and leaves the judgment to the manager, the same warn-don't-decide stance as the rest of this response. 
   * @return affectedUnassignedBookings
   */
  @NotNull @Valid 
  @JsonProperty("affectedUnassignedBookings")
  public List<@Valid RoomUnitBlockAffectedBooking> getAffectedUnassignedBookings() {
    return affectedUnassignedBookings;
  }

  public void setAffectedUnassignedBookings(List<@Valid RoomUnitBlockAffectedBooking> affectedUnassignedBookings) {
    this.affectedUnassignedBookings = affectedUnassignedBookings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomUnitBlockResult roomUnitBlockResult = (RoomUnitBlockResult) o;
    return Objects.equals(this.block, roomUnitBlockResult.block) &&
        Objects.equals(this.warning, roomUnitBlockResult.warning) &&
        Objects.equals(this.affectedBookings, roomUnitBlockResult.affectedBookings) &&
        Objects.equals(this.affectedUnassignedBookings, roomUnitBlockResult.affectedUnassignedBookings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(block, warning, affectedBookings, affectedUnassignedBookings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomUnitBlockResult {\n");
    sb.append("    block: ").append(toIndentedString(block)).append("\n");
    sb.append("    warning: ").append(toIndentedString(warning)).append("\n");
    sb.append("    affectedBookings: ").append(toIndentedString(affectedBookings)).append("\n");
    sb.append("    affectedUnassignedBookings: ").append(toIndentedString(affectedUnassignedBookings)).append("\n");
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

