package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import java.time.OffsetDateTime;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * One version of one shift code, optionally scoped to one &#x60;StaffArea&#x60; - never edited once any &#x60;RosterEntry&#x60; references it. &#x60;startTime1&#x60;/&#x60;endTime1&#x60; is the first (or only) interval; &#x60;startTime2&#x60;/&#x60;endTime2&#x60; is present only for a split shift (both null together, never one without the other). Zero intervals (all four null) means &#x60;OP&#x60; - worked, with no fixed hours. &#x60;countsAsWorked&#x60; and &#x60;isPaid&#x60; are independent: an ordinary shift is both true; &#x60;OP&#x60; is &#x60;countsAsWorked&#x60; true with no intervals; &#x60;PH&#x60; is &#x60;isPaid&#x60; true, &#x60;countsAsWorked&#x60; false - this is what removes the ambiguity of \&quot;holiday, annual leave, or a kept day off\&quot; being one label on the grid without splitting them on screen. A null &#x60;staffArea&#x60; means this code is shared: available to every area, one row instead of one per area. A non-null &#x60;staffArea&#x60; means this code means something different there than it does anywhere else - a shared and an area-scoped row may share the same &#x60;code&#x60; string at once, and where they do, the area-scoped one wins for that area (see &#x60;GET /shift-codes&#x60;&#39;s own description). Area-scoping stays fully supported (correct, tested, cheaper to keep than remove), but every code at this hotel is in fact shared today - the accountant confirmed nothing here actually varies by department; an earlier revision of this schema assumed otherwise for \&quot;9\&quot; specifically, and was wrong (see below). &#x60;active&#x3D;false&#x60; means retired: no longer offered for a *new* entry, but every entry already pointing at this row is untouched.  **\&quot;9\&quot; is two distinct codes, not one code with two meanings.** The source spreadsheet used one label, \&quot;9\&quot;, for two genuinely different shifts and told them apart by cell fill colour, not by department: yellow \&quot;9\&quot; is a single 09:00-18:00 shift, light blue \&quot;9\&quot; is a split 09:00-13:00/16:00-21:00 shift. Colour carries no meaning of its own anywhere else in the legend - it exists solely so staff can tell these two shifts apart on paper. This hotel defines two distinct codes instead of one ambiguous \&quot;9\&quot;: &#x60;\&quot;9\&quot;&#x60; for the single 09:00-18:00 shift (was yellow), &#x60;\&quot;9S\&quot;&#x60; for the split 09:00-13:00/16:00-21:00 shift (was light blue) - &#x60;S&#x60; for split, chosen to stay recognisable as \&quot;the other 9\&quot; to anyone who worked from the old paper legend. A future spreadsheet importer should read cell fill colour only when a cell&#39;s text is \&quot;9\&quot; (ignore fill everywhere else) and translate yellow to &#x60;\&quot;9\&quot;&#x60;, light blue to &#x60;\&quot;9S\&quot;&#x60; - and reject, not guess, a \&quot;9\&quot; cell whose fill matches neither known colour. A wrongly-guessed shift is a person turning up at the wrong hour. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class ShiftCode {

  private String id;

  private JsonNullable<StaffArea> staffArea = JsonNullable.<StaffArea>undefined();

  private String code;

  private JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> startTime1 = JsonNullable.<String>undefined();

  private JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> endTime1 = JsonNullable.<String>undefined();

  private JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> startTime2 = JsonNullable.<String>undefined();

  private JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> endTime2 = JsonNullable.<String>undefined();

  private Boolean countsAsWorked;

  private Boolean isPaid;

  private String effectiveFrom;

  private Boolean active;

  private String createdByEmail;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  private JsonNullable<ShiftCodeKind> kind = JsonNullable.<ShiftCodeKind>undefined();

  private JsonNullable<ShiftCodeKind> suggestedKind = JsonNullable.<ShiftCodeKind>undefined();

  public ShiftCode() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ShiftCode(String id, String code, Boolean countsAsWorked, Boolean isPaid, String effectiveFrom, Boolean active, String createdByEmail, OffsetDateTime createdAt) {
    this.id = id;
    this.code = code;
    this.countsAsWorked = countsAsWorked;
    this.isPaid = isPaid;
    this.effectiveFrom = effectiveFrom;
    this.active = active;
    this.createdByEmail = createdByEmail;
    this.createdAt = createdAt;
  }

  public ShiftCode id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ShiftCode staffArea(StaffArea staffArea) {
    this.staffArea = JsonNullable.of(staffArea);
    return this;
  }

  /**
   * Null means shared - available to every area. See this schema's own description.
   * @return staffArea
   */
  @Valid 
  @JsonProperty("staffArea")
  public JsonNullable<StaffArea> getStaffArea() {
    return staffArea;
  }

  public void setStaffArea(JsonNullable<StaffArea> staffArea) {
    this.staffArea = staffArea;
  }

  public ShiftCode code(String code) {
    this.code = code;
    return this;
  }

  /**
   * The short label as staff already know it - \"9\", \"8.3\", \"OP\", \"PH\".
   * @return code
   */
  @NotNull 
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public ShiftCode startTime1(String startTime1) {
    this.startTime1 = JsonNullable.of(startTime1);
    return this;
  }

  /**
   * Get startTime1
   * @return startTime1
   */
  @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("startTime1")
  public JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> getStartTime1() {
    return startTime1;
  }

  public void setStartTime1(JsonNullable<String> startTime1) {
    this.startTime1 = startTime1;
  }

  public ShiftCode endTime1(String endTime1) {
    this.endTime1 = JsonNullable.of(endTime1);
    return this;
  }

  /**
   * Get endTime1
   * @return endTime1
   */
  @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("endTime1")
  public JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> getEndTime1() {
    return endTime1;
  }

  public void setEndTime1(JsonNullable<String> endTime1) {
    this.endTime1 = endTime1;
  }

  public ShiftCode startTime2(String startTime2) {
    this.startTime2 = JsonNullable.of(startTime2);
    return this;
  }

  /**
   * Get startTime2
   * @return startTime2
   */
  @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("startTime2")
  public JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> getStartTime2() {
    return startTime2;
  }

  public void setStartTime2(JsonNullable<String> startTime2) {
    this.startTime2 = startTime2;
  }

  public ShiftCode endTime2(String endTime2) {
    this.endTime2 = JsonNullable.of(endTime2);
    return this;
  }

  /**
   * Get endTime2
   * @return endTime2
   */
  @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") 
  @JsonProperty("endTime2")
  public JsonNullable<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String> getEndTime2() {
    return endTime2;
  }

  public void setEndTime2(JsonNullable<String> endTime2) {
    this.endTime2 = endTime2;
  }

  public ShiftCode countsAsWorked(Boolean countsAsWorked) {
    this.countsAsWorked = countsAsWorked;
    return this;
  }

  /**
   * Get countsAsWorked
   * @return countsAsWorked
   */
  @NotNull 
  @JsonProperty("countsAsWorked")
  public Boolean getCountsAsWorked() {
    return countsAsWorked;
  }

  public void setCountsAsWorked(Boolean countsAsWorked) {
    this.countsAsWorked = countsAsWorked;
  }

  public ShiftCode isPaid(Boolean isPaid) {
    this.isPaid = isPaid;
    return this;
  }

  /**
   * Get isPaid
   * @return isPaid
   */
  @NotNull 
  @JsonProperty("isPaid")
  public Boolean getIsPaid() {
    return isPaid;
  }

  public void setIsPaid(Boolean isPaid) {
    this.isPaid = isPaid;
  }

  public ShiftCode effectiveFrom(String effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
    return this;
  }

  /**
   * Get effectiveFrom
   * @return effectiveFrom
   */
  @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") 
  @JsonProperty("effectiveFrom")
  public String getEffectiveFrom() {
    return effectiveFrom;
  }

  public void setEffectiveFrom(String effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }

  public ShiftCode active(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * Get active
   * @return active
   */
  @NotNull 
  @JsonProperty("active")
  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public ShiftCode createdByEmail(String createdByEmail) {
    this.createdByEmail = createdByEmail;
    return this;
  }

  /**
   * Get createdByEmail
   * @return createdByEmail
   */
  @NotNull 
  @JsonProperty("createdByEmail")
  public String getCreatedByEmail() {
    return createdByEmail;
  }

  public void setCreatedByEmail(String createdByEmail) {
    this.createdByEmail = createdByEmail;
  }

  public ShiftCode createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public ShiftCode kind(ShiftCodeKind kind) {
    this.kind = JsonNullable.of(kind);
    return this;
  }

  /**
   * Null only for a code created before this field existed and not yet confirmed - see `suggestedKind` and `PATCH /shift-codes/{id}/kind`. Every code created from here on has one (see `ShiftCodeCreateInput`). 
   * @return kind
   */
  @Valid 
  @JsonProperty("kind")
  public JsonNullable<ShiftCodeKind> getKind() {
    return kind;
  }

  public void setKind(JsonNullable<ShiftCodeKind> kind) {
    this.kind = kind;
  }

  public ShiftCode suggestedKind(ShiftCodeKind suggestedKind) {
    this.suggestedKind = JsonNullable.of(suggestedKind);
    return this;
  }

  /**
   * A default guessed from this code's own interval shape and `countsAsWorked` - set only when `kind` is null, for `PATCH /shift-codes/{id}/kind` to offer as a starting point, never applied on its own. Same \"suggest, don't silently apply\" shape as `RosterImportNameEntry.suggestedStaffArea`. 
   * @return suggestedKind
   */
  @Valid 
  @JsonProperty("suggestedKind")
  public JsonNullable<ShiftCodeKind> getSuggestedKind() {
    return suggestedKind;
  }

  public void setSuggestedKind(JsonNullable<ShiftCodeKind> suggestedKind) {
    this.suggestedKind = suggestedKind;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShiftCode shiftCode = (ShiftCode) o;
    return Objects.equals(this.id, shiftCode.id) &&
        equalsNullable(this.staffArea, shiftCode.staffArea) &&
        Objects.equals(this.code, shiftCode.code) &&
        equalsNullable(this.startTime1, shiftCode.startTime1) &&
        equalsNullable(this.endTime1, shiftCode.endTime1) &&
        equalsNullable(this.startTime2, shiftCode.startTime2) &&
        equalsNullable(this.endTime2, shiftCode.endTime2) &&
        Objects.equals(this.countsAsWorked, shiftCode.countsAsWorked) &&
        Objects.equals(this.isPaid, shiftCode.isPaid) &&
        Objects.equals(this.effectiveFrom, shiftCode.effectiveFrom) &&
        Objects.equals(this.active, shiftCode.active) &&
        Objects.equals(this.createdByEmail, shiftCode.createdByEmail) &&
        Objects.equals(this.createdAt, shiftCode.createdAt) &&
        equalsNullable(this.kind, shiftCode.kind) &&
        equalsNullable(this.suggestedKind, shiftCode.suggestedKind);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, hashCodeNullable(staffArea), code, hashCodeNullable(startTime1), hashCodeNullable(endTime1), hashCodeNullable(startTime2), hashCodeNullable(endTime2), countsAsWorked, isPaid, effectiveFrom, active, createdByEmail, createdAt, hashCodeNullable(kind), hashCodeNullable(suggestedKind));
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
    sb.append("class ShiftCode {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    staffArea: ").append(toIndentedString(staffArea)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    startTime1: ").append(toIndentedString(startTime1)).append("\n");
    sb.append("    endTime1: ").append(toIndentedString(endTime1)).append("\n");
    sb.append("    startTime2: ").append(toIndentedString(startTime2)).append("\n");
    sb.append("    endTime2: ").append(toIndentedString(endTime2)).append("\n");
    sb.append("    countsAsWorked: ").append(toIndentedString(countsAsWorked)).append("\n");
    sb.append("    isPaid: ").append(toIndentedString(isPaid)).append("\n");
    sb.append("    effectiveFrom: ").append(toIndentedString(effectiveFrom)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    createdByEmail: ").append(toIndentedString(createdByEmail)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    suggestedKind: ").append(toIndentedString(suggestedKind)).append("\n");
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

