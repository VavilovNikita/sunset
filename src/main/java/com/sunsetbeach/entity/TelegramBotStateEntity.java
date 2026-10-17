package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The single row of Telegram polling state (id 1); see V148.
 */
@Entity
@Table(name = "TelegramBotState")
public class TelegramBotStateEntity {

    @Id
    private Integer id;

    private long updateOffset;

    private String botUsername;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public long getUpdateOffset() {
        return updateOffset;
    }

    public void setUpdateOffset(long updateOffset) {
        this.updateOffset = updateOffset;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public void setBotUsername(String botUsername) {
        this.botUsername = botUsername;
    }

}
