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
 * NotificationStatus
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.10.0")
public class NotificationStatus {

  private Boolean telegramConfigured;

  private JsonNullable<String> botUsername = JsonNullable.<String>undefined();

  public NotificationStatus() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NotificationStatus(Boolean telegramConfigured) {
    this.telegramConfigured = telegramConfigured;
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
        equalsNullable(this.botUsername, notificationStatus.botUsername);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(telegramConfigured, hashCodeNullable(botUsername));
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

