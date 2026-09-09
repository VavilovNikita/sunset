package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
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
 * One entry of the array body of &#x60;PATCH /tables/positions&#x60; - mirrors &#x60;RoomUnitPositionInput&#x60; exactly, see that schema&#39;s own comment for why &#x60;positionX&#x60;/ &#x60;positionY&#x60; aren&#39;t under &#x60;required&#x60; despite conceptually needing to be present. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class TablePositionInput {

  private String tableId;

  private JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> positionX = JsonNullable.<BigDecimal>undefined();

  private JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> positionY = JsonNullable.<BigDecimal>undefined();

  public TablePositionInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TablePositionInput(String tableId) {
    this.tableId = tableId;
  }

  public TablePositionInput tableId(String tableId) {
    this.tableId = tableId;
    return this;
  }

  /**
   * Get tableId
   * @return tableId
   */
  @NotNull 
  @JsonProperty("tableId")
  public String getTableId() {
    return tableId;
  }

  public void setTableId(String tableId) {
    this.tableId = tableId;
  }

  public TablePositionInput positionX(BigDecimal positionX) {
    this.positionX = JsonNullable.of(positionX);
    return this;
  }

  /**
   * Get positionX
   * minimum: 0
   * maximum: 1
   * @return positionX
   */
  @Valid @DecimalMin("0") @DecimalMax("1") 
  @JsonProperty("positionX")
  public JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> getPositionX() {
    return positionX;
  }

  public void setPositionX(JsonNullable<BigDecimal> positionX) {
    this.positionX = positionX;
  }

  public TablePositionInput positionY(BigDecimal positionY) {
    this.positionY = JsonNullable.of(positionY);
    return this;
  }

  /**
   * Get positionY
   * minimum: 0
   * maximum: 1
   * @return positionY
   */
  @Valid @DecimalMin("0") @DecimalMax("1") 
  @JsonProperty("positionY")
  public JsonNullable<@DecimalMin("0") @DecimalMax("1") BigDecimal> getPositionY() {
    return positionY;
  }

  public void setPositionY(JsonNullable<BigDecimal> positionY) {
    this.positionY = positionY;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TablePositionInput tablePositionInput = (TablePositionInput) o;
    return Objects.equals(this.tableId, tablePositionInput.tableId) &&
        equalsNullable(this.positionX, tablePositionInput.positionX) &&
        equalsNullable(this.positionY, tablePositionInput.positionY);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableId, hashCodeNullable(positionX), hashCodeNullable(positionY));
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
    sb.append("class TablePositionInput {\n");
    sb.append("    tableId: ").append(toIndentedString(tableId)).append("\n");
    sb.append("    positionX: ").append(toIndentedString(positionX)).append("\n");
    sb.append("    positionY: ").append(toIndentedString(positionY)).append("\n");
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

