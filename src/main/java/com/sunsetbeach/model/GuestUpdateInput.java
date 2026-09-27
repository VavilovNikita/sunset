package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Body of &#x60;PATCH /guests/{id}&#x60;. Full replacement of every field below, same convention as &#x60;TableInput&#x60;/&#x60;RoomUnitUpdateInput&#x60; - no partial update. &#x60;vip&#x60; and &#x60;tags&#x60; are required (not defaulted, unlike &#x60;GuestCreateInput&#x60;): a caller that doesn&#39;t know about them must get a 400, not silently unmark a VIP or wipe their tags. Only &#x60;vip&#x60;&#39;s requirement is actually enforced - the generated model initializes &#x60;tags&#x60; to an empty list, so an omitted &#x60;tags&#x60; is indistinguishable from &#x60;[]&#x60; - but that is enough: any caller unaware of these fields also omits &#x60;vip&#x60; and is rejected before its missing &#x60;tags&#x60; could clear anything. &#x60;dateOfBirth&#x60; follows &#x60;email&#x60;/&#x60;phone&#x60;/&#x60;notes&#x60; - omitted clears it. Same &#x60;dateOfBirth&#x60;/&#x60;tags&#x60; rules as &#x60;GuestCreateInput&#x60;. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class GuestUpdateInput {

  private String name;

  private JsonNullable<String> email = JsonNullable.<String>undefined();

  private JsonNullable<String> phone = JsonNullable.<String>undefined();

  private JsonNullable<@Size(max = 2000) String> notes = JsonNullable.<String>undefined();

  private Boolean vip;

  private JsonNullable<@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") String> dateOfBirth = JsonNullable.<String>undefined();

  @Valid
  private List<@Size(max = 50)String> tags = new ArrayList<>();

  public GuestUpdateInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GuestUpdateInput(String name, Boolean vip, List<@Size(max = 50)String> tags) {
    this.name = name;
    this.vip = vip;
    this.tags = tags;
  }

  public GuestUpdateInput name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(min = 1, max = 120) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public GuestUpdateInput email(String email) {
    this.email = JsonNullable.of(email);
    return this;
  }

  /**
   * Get email
   * @return email
   */
  
  @JsonProperty("email")
  public JsonNullable<String> getEmail() {
    return email;
  }

  public void setEmail(JsonNullable<String> email) {
    this.email = email;
  }

  public GuestUpdateInput phone(String phone) {
    this.phone = JsonNullable.of(phone);
    return this;
  }

  /**
   * Get phone
   * @return phone
   */
  
  @JsonProperty("phone")
  public JsonNullable<String> getPhone() {
    return phone;
  }

  public void setPhone(JsonNullable<String> phone) {
    this.phone = phone;
  }

  public GuestUpdateInput notes(String notes) {
    this.notes = JsonNullable.of(notes);
    return this;
  }

  /**
   * Get notes
   * @return notes
   */
  @Size(max = 2000) 
  @JsonProperty("notes")
  public JsonNullable<@Size(max = 2000) String> getNotes() {
    return notes;
  }

  public void setNotes(JsonNullable<String> notes) {
    this.notes = notes;
  }

  public GuestUpdateInput vip(Boolean vip) {
    this.vip = vip;
    return this;
  }

  /**
   * Get vip
   * @return vip
   */
  @NotNull 
  @JsonProperty("vip")
  public Boolean getVip() {
    return vip;
  }

  public void setVip(Boolean vip) {
    this.vip = vip;
  }

  public GuestUpdateInput dateOfBirth(String dateOfBirth) {
    this.dateOfBirth = JsonNullable.of(dateOfBirth);
    return this;
  }

  /**
   * Get dateOfBirth
   * @return dateOfBirth
   */
  @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("dateOfBirth")
  public JsonNullable<@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") String> getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(JsonNullable<String> dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public GuestUpdateInput tags(List<@Size(max = 50)String> tags) {
    this.tags = tags;
    return this;
  }

  public GuestUpdateInput addTagsItem(String tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Get tags
   * @return tags
   */
  @NotNull @Size(max = 20) 
  @JsonProperty("tags")
  public List<@Size(max = 50)String> getTags() {
    return tags;
  }

  public void setTags(List<@Size(max = 50)String> tags) {
    this.tags = tags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuestUpdateInput guestUpdateInput = (GuestUpdateInput) o;
    return Objects.equals(this.name, guestUpdateInput.name) &&
        equalsNullable(this.email, guestUpdateInput.email) &&
        equalsNullable(this.phone, guestUpdateInput.phone) &&
        equalsNullable(this.notes, guestUpdateInput.notes) &&
        Objects.equals(this.vip, guestUpdateInput.vip) &&
        equalsNullable(this.dateOfBirth, guestUpdateInput.dateOfBirth) &&
        Objects.equals(this.tags, guestUpdateInput.tags);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, hashCodeNullable(email), hashCodeNullable(phone), hashCodeNullable(notes), vip, hashCodeNullable(dateOfBirth), tags);
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
    sb.append("class GuestUpdateInput {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    email: ").append("[REDACTED]").append("\n");
    sb.append("    phone: ").append("[REDACTED]").append("\n");
    sb.append("    notes: ").append("[REDACTED]").append("\n");
    sb.append("    vip: ").append(toIndentedString(vip)).append("\n");
    sb.append("    dateOfBirth: ").append("[REDACTED]").append("\n");
    sb.append("    tags: ").append("[REDACTED]").append("\n");
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

