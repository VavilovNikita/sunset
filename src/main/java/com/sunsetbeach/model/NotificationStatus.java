package com.sunsetbeach.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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

  /**
   * How requests to Telegram go out right now. `PROXY` = through the hotel's HTTP proxy (`TELEGRAM_PROXY`), `DIRECT` = straight from the server. With a proxy configured, `DIRECT` means the server fell back after repeated failed polls and is probing the proxy again every few minutes; without one it is always `DIRECT`. 
   */
  public enum TelegramRouteEnum {
    DIRECT("DIRECT"),
    
    PROXY("PROXY");

    private String value;

    TelegramRouteEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TelegramRouteEnum fromValue(String value) {
      for (TelegramRouteEnum b : TelegramRouteEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private TelegramRouteEnum telegramRoute;

  private Boolean telegramProxyConfigured;

  public NotificationStatus() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NotificationStatus(Boolean telegramConfigured, Integer consecutiveFailures, TelegramRouteEnum telegramRoute, Boolean telegramProxyConfigured) {
    this.telegramConfigured = telegramConfigured;
    this.consecutiveFailures = consecutiveFailures;
    this.telegramRoute = telegramRoute;
    this.telegramProxyConfigured = telegramProxyConfigured;
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

  public NotificationStatus telegramRoute(TelegramRouteEnum telegramRoute) {
    this.telegramRoute = telegramRoute;
    return this;
  }

  /**
   * How requests to Telegram go out right now. `PROXY` = through the hotel's HTTP proxy (`TELEGRAM_PROXY`), `DIRECT` = straight from the server. With a proxy configured, `DIRECT` means the server fell back after repeated failed polls and is probing the proxy again every few minutes; without one it is always `DIRECT`. 
   * @return telegramRoute
   */
  @NotNull 
  @JsonProperty("telegramRoute")
  public TelegramRouteEnum getTelegramRoute() {
    return telegramRoute;
  }

  public void setTelegramRoute(TelegramRouteEnum telegramRoute) {
    this.telegramRoute = telegramRoute;
  }

  public NotificationStatus telegramProxyConfigured(Boolean telegramProxyConfigured) {
    this.telegramProxyConfigured = telegramProxyConfigured;
    return this;
  }

  /**
   * Whether `TELEGRAM_PROXY` is set. The proxy's address is never returned.
   * @return telegramProxyConfigured
   */
  @NotNull 
  @JsonProperty("telegramProxyConfigured")
  public Boolean getTelegramProxyConfigured() {
    return telegramProxyConfigured;
  }

  public void setTelegramProxyConfigured(Boolean telegramProxyConfigured) {
    this.telegramProxyConfigured = telegramProxyConfigured;
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
        Objects.equals(this.consecutiveFailures, notificationStatus.consecutiveFailures) &&
        Objects.equals(this.telegramRoute, notificationStatus.telegramRoute) &&
        Objects.equals(this.telegramProxyConfigured, notificationStatus.telegramProxyConfigured);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(telegramConfigured, hashCodeNullable(botUsername), hashCodeNullable(lastSuccessAt), hashCodeNullable(failingSince), consecutiveFailures, telegramRoute, telegramProxyConfigured);
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
    sb.append("    telegramRoute: ").append(toIndentedString(telegramRoute)).append("\n");
    sb.append("    telegramProxyConfigured: ").append(toIndentedString(telegramProxyConfigured)).append("\n");
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

