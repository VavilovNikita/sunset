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
 * At least one of &#x60;tableId&#x60;/&#x60;bookingId&#x60;/&#x60;guestName&#x60; is expected in practice (an order with none of them is a valid but untraceable tab), but this isn&#39;t enforced server-side — same \&quot;trust the caller on shape, not on price\&quot; spirit as elsewhere.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class OrderCreateInput {

  private JsonNullable<String> tableId = JsonNullable.<String>undefined();

  private JsonNullable<String> bookingId = JsonNullable.<String>undefined();

  private JsonNullable<@Size(max = 120) String> guestName = JsonNullable.<String>undefined();

  private JsonNullable<String> spaAppointmentId = JsonNullable.<String>undefined();

  public OrderCreateInput tableId(String tableId) {
    this.tableId = JsonNullable.of(tableId);
    return this;
  }

  /**
   * Get tableId
   * @return tableId
   */
  
  @JsonProperty("tableId")
  public JsonNullable<String> getTableId() {
    return tableId;
  }

  public void setTableId(JsonNullable<String> tableId) {
    this.tableId = tableId;
  }

  public OrderCreateInput bookingId(String bookingId) {
    this.bookingId = JsonNullable.of(bookingId);
    return this;
  }

  /**
   * Becomes `Order.bookingId` - see that field's own description for why sending this is not how a booking ends up billed for the order. Charging an order to a room happens at `POST /orders/{id}/close` with `method: ROOM_CHARGE` and a `bookingId` in that body, independently of whatever (if anything) was sent here at creation. 
   * @return bookingId
   */
  
  @JsonProperty("bookingId")
  public JsonNullable<String> getBookingId() {
    return bookingId;
  }

  public void setBookingId(JsonNullable<String> bookingId) {
    this.bookingId = bookingId;
  }

  public OrderCreateInput guestName(String guestName) {
    this.guestName = JsonNullable.of(guestName);
    return this;
  }

  /**
   * Get guestName
   * @return guestName
   */
  @Size(max = 120) 
  @JsonProperty("guestName")
  public JsonNullable<@Size(max = 120) String> getGuestName() {
    return guestName;
  }

  public void setGuestName(JsonNullable<String> guestName) {
    this.guestName = guestName;
  }

  public OrderCreateInput spaAppointmentId(String spaAppointmentId) {
    this.spaAppointmentId = JsonNullable.of(spaAppointmentId);
    return this;
  }

  /**
   * When set, this order becomes `SpaAppointment.orderId` for that appointment (see that field's own description; the appointment itself never computes or stores an amount). An id that doesn't resolve to a real appointment is silently ignored, same \"don't let a side link fail the write it rides on\" spirit as printing/audit - order creation is never blocked by this. Unconditional: sending this always links, regardless of the order's table/booking/items - it's the override for when auto-resolution below can't or shouldn't guess. Usually left unset - auto-resolution covers the ordinary case without it, and only once the order actually contains a `SPA`-department item - true of every order that genuinely bills a treatment, false of everything else, so a table-less order or a spa-table order for something else can never link just because its table/booking happens to coincide with a real appointment. Gated the same way on both of the two axes below, tried at two different moments because each needs a fact that doesn't exist at the other's call site: By `tableId`, attempted whenever items are added (`POST /orders/{id}/items`, see `OrderService#autoLinkSpaAppointmentByTable`) - the table is known from the moment the order opens, but the SPA-content gate needs items, which don't exist until one has been rung in. Matched by *time*, not count - the appointment on that table whose slot contains the moment the order was opened, or (covering the ordinary case of items being rung in a few minutes after the treatment actually ends) one that ended within the last `app.spa.order-link-grace-minutes`. By the booking a `ROOM_CHARGE` close names, attempted at `POST /orders/{id}/close` (see `OrderService#autoLinkSpaAppointmentByBooking`) - a room-charge order often carries no `tableId` at all, and closing is the one moment a booking is actually known for one: `Order.bookingId` (settable only via an optional field on this schema) is never populated by any real caller today, so this axis never used the field this schema exposes - it reads the id the close body itself names instead. Count-based, not time-based (no slot to match against at close): exactly one of the booking's unlinked appointments today is unambiguous, two or more decline. A resolution failure here never affects the close it rides on, same as it never affects `addItems` above. Either axis declines rather than guesses when it finds more than one candidate, and neither re-resolves an order the other has already linked. Send `spaAppointmentId` explicitly to override auto-resolution on either axis, or when both declined and staff need to link by hand. 
   * @return spaAppointmentId
   */
  
  @JsonProperty("spaAppointmentId")
  public JsonNullable<String> getSpaAppointmentId() {
    return spaAppointmentId;
  }

  public void setSpaAppointmentId(JsonNullable<String> spaAppointmentId) {
    this.spaAppointmentId = spaAppointmentId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderCreateInput orderCreateInput = (OrderCreateInput) o;
    return equalsNullable(this.tableId, orderCreateInput.tableId) &&
        equalsNullable(this.bookingId, orderCreateInput.bookingId) &&
        equalsNullable(this.guestName, orderCreateInput.guestName) &&
        equalsNullable(this.spaAppointmentId, orderCreateInput.spaAppointmentId);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(tableId), hashCodeNullable(bookingId), hashCodeNullable(guestName), hashCodeNullable(spaAppointmentId));
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
    sb.append("class OrderCreateInput {\n");
    sb.append("    tableId: ").append(toIndentedString(tableId)).append("\n");
    sb.append("    bookingId: ").append(toIndentedString(bookingId)).append("\n");
    sb.append("    guestName: ").append(toIndentedString(guestName)).append("\n");
    sb.append("    spaAppointmentId: ").append(toIndentedString(spaAppointmentId)).append("\n");
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

