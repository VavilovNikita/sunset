package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * NotificationStatus
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class NotificationStatus {

  private Boolean telegramConfigured;

  private JsonNullable<String> botUsername = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> lastSuccessAt = JsonNullable.<OffsetDateTime>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> failingSince = JsonNullable.<OffsetDateTime>undefined();

  private Integer consecutiveFailures;

  public NotificationStatus() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NotificationStatus(Boolean telegramConfigured, Integer consecutiveFailures) {
    this.telegramConfigured = telegramConfigured;
    this.consecutiveFailures = consecutiveFailures;
  }

  public NotificationStatus telegramConfigured(Boolean telegramConfigured) {
    this.telegramConfigured = telegramConfigured;
    return this;
  }

  /**
   * False when the server has no `TELEGRAM_BOT_TOKEN` - nothing is polled or sent then.
   * @return telegramConfigured
   */
  @NotNull 
  @JsonProperty("telegramConfigured")
  public Boolean getTelegramConfigured() {
    return telegramConfigured;
  }

  public void setTelegramConfigured(Boolean telegramConfigured) {
    this.telegramConfigured = telegramConfigured;
  }

  public NotificationStatus botUsername(String botUsername) {
    this.botUsername = JsonNullable.of(botUsername);
    return this;
  }

  /**
   * The bot's name from Telegram's `getMe`, without the `@`. Null while unknown (or when not configured).
   * @return botUsername
   */
  
  @JsonProperty("botUsername")
  public JsonNullable<String> getBotUsername() {
    return botUsername;
  }

  public void setBotUsername(JsonNullable<String> botUsername) {
    this.botUsername = botUsername;
  }

  public NotificationStatus lastSuccessAt(OffsetDateTime lastSuccessAt) {
    this.lastSuccessAt = JsonNullable.of(lastSuccessAt);
    return this;
  }

  /**
   * When Telegram last answered the server's poll (`getUpdates`). Absent until the first answer since the server started (in memory only, so it also starts empty after a restart) and when Telegram is not configured. 
   * @return lastSuccessAt
   */
  @Valid 
  @JsonProperty("lastSuccessAt")
  public JsonNullable<OffsetDateTime> getLastSuccessAt() {
    return lastSuccessAt;
  }

  public void setLastSuccessAt(JsonNullable<OffsetDateTime> lastSuccessAt) {
    this.lastSuccessAt = lastSuccessAt;
  }

  public NotificationStatus failingSince(OffsetDateTime failingSince) {
    this.failingSince = JsonNullable.of(failingSince);
    return this;
  }

  /**
   * When the current series of failed polls began. Absent while Telegram answers.
   * @return failingSince
   */
  @Valid 
  @JsonProperty("failingSince")
  public JsonNullable<OffsetDateTime> getFailingSince() {
    return failingSince;
  }

  public void setFailingSince(JsonNullable<OffsetDateTime> failingSince) {
    this.failingSince = failingSince;
  }

  public NotificationStatus consecutiveFailures(Integer consecutiveFailures) {
    this.consecutiveFailures = consecutiveFailures;
    return this;
  }

  /**
   * Polls in a row that failed. 0 while Telegram answers; above 0 means a series is running and the server is backing off (3 s, doubling, up to 60 s).
   * minimum: 0
   * @return consecutiveFailures
   */
  @NotNull @Min(0) 
  @JsonProperty("consecutiveFailures")
  public Integer getConsecutiveFailures() {
    return consecutiveFailures;
  }

  public void setConsecutiveFailures(Integer consecutiveFailures) {
    this.consecutiveFailures = consecutiveFailures;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationStatus notificationStatus = (NotificationStatus) o;
    return Objects.equals(this.telegramConfigured, notificationStatus.telegramConfigured) &&
        equalsNullable(this.botUsername, notificationStatus.botUsername) &&
        equalsNullable(this.lastSuccessAt, notificationStatus.lastSuccessAt) &&
        equalsNullable(this.failingSince, notificationStatus.failingSince) &&
        Objects.equals(this.consecutiveFailures, notificationStatus.consecutiveFailures);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(telegramConfigured, hashCodeNullable(botUsername), hashCodeNullable(lastSuccessAt), hashCodeNullable(failingSince), consecutiveFailures);
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
    sb.append("class NotificationStatus {\n");
    sb.append("    telegramConfigured: ").append(toIndentedString(telegramConfigured)).append("\n");
    sb.append("    botUsername: ").append(toIndentedString(botUsername)).append("\n");
    sb.append("    lastSuccessAt: ").append(toIndentedString(lastSuccessAt)).append("\n");
    sb.append("    failingSince: ").append(toIndentedString(failingSince)).append("\n");
    sb.append("    consecutiveFailures: ").append(toIndentedString(consecutiveFailures)).append("\n");
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

