package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.SiteMinderReservationStatus;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One reservation as read off SiteMinder&#39;s reservation list/card, sent by the polling script. No guest email or phone - SiteMinder masks them until trusted-device access is set up; a later enrichment pass will find the booking by &#x60;reference&#x60; and fill them in on its &#x60;Guest&#x60; card. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class SiteMinderReservationInput {

  private String reference;

  private SiteMinderReservationStatus status;

  private String firstName;

  private String lastName;

  private String checkIn;

  private String checkOut;

  private String roomTypeName;

  private Integer adults;

  private Integer children = 0;

  private Integer infants = 0;

  private String totalPrice;

  private String currency;

  private String channel;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime bookedAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime modifiedAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime cancelledAt;

  public SiteMinderReservationInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SiteMinderReservationInput(String reference, SiteMinderReservationStatus status, String lastName, String checkIn, String checkOut, String roomTypeName, Integer adults, String totalPrice, String channel, OffsetDateTime bookedAt) {
    this.reference = reference;
    this.status = status;
    this.lastName = lastName;
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.roomTypeName = roomTypeName;
    this.adults = adults;
    this.totalPrice = totalPrice;
    this.channel = channel;
    this.bookedAt = bookedAt;
  }

  public SiteMinderReservationInput reference(String reference) {
    this.reference = reference;
    return this;
  }

  /**
   * SiteMinder's own booking reference - the one shown on both the list and the card view, not the per-OTA reference. The idempotency key. 
   * @return reference
   */
  @NotNull @Size(min = 1, max = 100) 
  @JsonProperty("reference")
  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public SiteMinderReservationInput status(SiteMinderReservationStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @JsonProperty("status")
  public SiteMinderReservationStatus getStatus() {
    return status;
  }

  public void setStatus(SiteMinderReservationStatus status) {
    this.status = status;
  }

  public SiteMinderReservationInput firstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  /**
   * Get firstName
   * @return firstName
   */
  @Size(max = 120) 
  @JsonProperty("firstName")
  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public SiteMinderReservationInput lastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  /**
   * Get lastName
   * @return lastName
   */
  @NotNull @Size(min = 1, max = 120) 
  @JsonProperty("lastName")
  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public SiteMinderReservationInput checkIn(String checkIn) {
    this.checkIn = checkIn;
    return this;
  }

  /**
   * Get checkIn
   * @return checkIn
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("checkIn")
  public String getCheckIn() {
    return checkIn;
  }

  public void setCheckIn(String checkIn) {
    this.checkIn = checkIn;
  }

  public SiteMinderReservationInput checkOut(String checkOut) {
    this.checkOut = checkOut;
    return this;
  }

  /**
   * Get checkOut
   * @return checkOut
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("checkOut")
  public String getCheckOut() {
    return checkOut;
  }

  public void setCheckOut(String checkOut) {
    this.checkOut = checkOut;
  }

  public SiteMinderReservationInput roomTypeName(String roomTypeName) {
    this.roomTypeName = roomTypeName;
    return this;
  }

  /**
   * SiteMinder's room type name exactly as displayed, e.g. \"Garden Jacuzzi Villa ABF\". Must be mapped - see `SiteMinderRoomTypeMapping`.
   * @return roomTypeName
   */
  @NotNull @Size(min = 1, max = 200) 
  @JsonProperty("roomTypeName")
  public String getRoomTypeName() {
    return roomTypeName;
  }

  public void setRoomTypeName(String roomTypeName) {
    this.roomTypeName = roomTypeName;
  }

  public SiteMinderReservationInput adults(Integer adults) {
    this.adults = adults;
    return this;
  }

  /**
   * Get adults
   * minimum: 1
   * @return adults
   */
  @NotNull @Min(1) 
  @JsonProperty("adults")
  public Integer getAdults() {
    return adults;
  }

  public void setAdults(Integer adults) {
    this.adults = adults;
  }

  public SiteMinderReservationInput children(Integer children) {
    this.children = children;
    return this;
  }

  /**
   * Get children
   * minimum: 0
   * @return children
   */
  @Min(0) 
  @JsonProperty("children")
  public Integer getChildren() {
    return children;
  }

  public void setChildren(Integer children) {
    this.children = children;
  }

  public SiteMinderReservationInput infants(Integer infants) {
    this.infants = infants;
    return this;
  }

  /**
   * Accepted but not stored - `Booking` has no infants count, and folding infants into `children` would change the party size every report counts. Named in the creation audit entry so it isn't lost entirely. 
   * minimum: 0
   * @return infants
   */
  @Min(0) 
  @JsonProperty("infants")
  public Integer getInfants() {
    return infants;
  }

  public void setInfants(Integer infants) {
    this.infants = infants;
  }

  public SiteMinderReservationInput totalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
    return this;
  }

  /**
   * SiteMinder's total for the stay as a decimal string in THB, e.g. `\"12500.00\"`. Becomes the booking's agreed price - see the operation description.
   * @return totalPrice
   */
  @NotNull @Pattern(regexp = "^\\d{1,8}(\\.\\d{1,2})?$") 
  @JsonProperty("totalPrice")
  public String getTotalPrice() {
    return totalPrice;
  }

  public void setTotalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
  }

  public SiteMinderReservationInput currency(String currency) {
    this.currency = currency;
    return this;
  }

  /**
   * Optional. If sent, must be `THB` - anything else is rejected, so a price in another currency can't be booked as baht.
   * @return currency
   */
  
  @JsonProperty("currency")
  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public SiteMinderReservationInput channel(String channel) {
    this.channel = channel;
    return this;
  }

  /**
   * The channel name as SiteMinder displays it. Mapped to `BookingChannel`: names starting with Booking.com, Expedia, Agoda or Airbnb map to those values; \"Direct\" and SiteMinder's own booking engine (the hotel's website) map to `DIRECT`; anything else maps to `OTHER`. The raw name is kept as `Booking.externalChannel` either way. 
   * @return channel
   */
  @NotNull @Size(min = 1, max = 100) 
  @JsonProperty("channel")
  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public SiteMinderReservationInput bookedAt(OffsetDateTime bookedAt) {
    this.bookedAt = bookedAt;
    return this;
  }

  /**
   * SiteMinder's \"Booked on\" time, with its UTC offset.
   * @return bookedAt
   */
  @NotNull @Valid 
  @JsonProperty("bookedAt")
  public OffsetDateTime getBookedAt() {
    return bookedAt;
  }

  public void setBookedAt(OffsetDateTime bookedAt) {
    this.bookedAt = bookedAt;
  }

  public SiteMinderReservationInput modifiedAt(OffsetDateTime modifiedAt) {
    this.modifiedAt = modifiedAt;
    return this;
  }

  /**
   * SiteMinder's \"Modified on\" time, if shown.
   * @return modifiedAt
   */
  @Valid 
  @JsonProperty("modifiedAt")
  public OffsetDateTime getModifiedAt() {
    return modifiedAt;
  }

  public void setModifiedAt(OffsetDateTime modifiedAt) {
    this.modifiedAt = modifiedAt;
  }

  public SiteMinderReservationInput cancelledAt(OffsetDateTime cancelledAt) {
    this.cancelledAt = cancelledAt;
    return this;
  }

  /**
   * SiteMinder's \"Cancelled on\" time, if shown.
   * @return cancelledAt
   */
  @Valid 
  @JsonProperty("cancelledAt")
  public OffsetDateTime getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(OffsetDateTime cancelledAt) {
    this.cancelledAt = cancelledAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SiteMinderReservationInput siteMinderReservationInput = (SiteMinderReservationInput) o;
    return Objects.equals(this.reference, siteMinderReservationInput.reference) &&
        Objects.equals(this.status, siteMinderReservationInput.status) &&
        Objects.equals(this.firstName, siteMinderReservationInput.firstName) &&
        Objects.equals(this.lastName, siteMinderReservationInput.lastName) &&
        Objects.equals(this.checkIn, siteMinderReservationInput.checkIn) &&
        Objects.equals(this.checkOut, siteMinderReservationInput.checkOut) &&
        Objects.equals(this.roomTypeName, siteMinderReservationInput.roomTypeName) &&
        Objects.equals(this.adults, siteMinderReservationInput.adults) &&
        Objects.equals(this.children, siteMinderReservationInput.children) &&
        Objects.equals(this.infants, siteMinderReservationInput.infants) &&
        Objects.equals(this.totalPrice, siteMinderReservationInput.totalPrice) &&
        Objects.equals(this.currency, siteMinderReservationInput.currency) &&
        Objects.equals(this.channel, siteMinderReservationInput.channel) &&
        Objects.equals(this.bookedAt, siteMinderReservationInput.bookedAt) &&
        Objects.equals(this.modifiedAt, siteMinderReservationInput.modifiedAt) &&
        Objects.equals(this.cancelledAt, siteMinderReservationInput.cancelledAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reference, status, firstName, lastName, checkIn, checkOut, roomTypeName, adults, children, infants, totalPrice, currency, channel, bookedAt, modifiedAt, cancelledAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SiteMinderReservationInput {\n");
    sb.append("    reference: ").append(toIndentedString(reference)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
    sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("    roomTypeName: ").append(toIndentedString(roomTypeName)).append("\n");
    sb.append("    adults: ").append(toIndentedString(adults)).append("\n");
    sb.append("    children: ").append(toIndentedString(children)).append("\n");
    sb.append("    infants: ").append(toIndentedString(infants)).append("\n");
    sb.append("    totalPrice: ").append(toIndentedString(totalPrice)).append("\n");
    sb.append("    currency: ").append(toIndentedString(currency)).append("\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    bookedAt: ").append(toIndentedString(bookedAt)).append("\n");
    sb.append("    modifiedAt: ").append(toIndentedString(modifiedAt)).append("\n");
    sb.append("    cancelledAt: ").append(toIndentedString(cancelledAt)).append("\n");
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

