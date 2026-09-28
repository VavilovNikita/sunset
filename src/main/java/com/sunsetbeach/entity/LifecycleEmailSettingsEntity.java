package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * The single row of lifecycle email settings - see V112's own comment. Its id is always
 * {@link #SINGLETON_ID}; the row is seeded by that migration and only ever updated, never created
 * or deleted by the app.
 */
@Entity
@Table(name = "LifecycleEmailSettings")
public class LifecycleEmailSettingsEntity {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    private boolean preArrivalEnabled;

    private int preArrivalDaysBefore;

    private boolean postStayEnabled;

    private int postStayDaysAfter;

    private String postStayReviewUrl;

    private boolean winBackEnabled;

    private int winBackMonthsSinceStay;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public boolean isPreArrivalEnabled() {
        return preArrivalEnabled;
    }

    public void setPreArrivalEnabled(boolean preArrivalEnabled) {
        this.preArrivalEnabled = preArrivalEnabled;
    }

    public int getPreArrivalDaysBefore() {
        return preArrivalDaysBefore;
    }

    public void setPreArrivalDaysBefore(int preArrivalDaysBefore) {
        this.preArrivalDaysBefore = preArrivalDaysBefore;
    }

    public boolean isPostStayEnabled() {
        return postStayEnabled;
    }

    public void setPostStayEnabled(boolean postStayEnabled) {
        this.postStayEnabled = postStayEnabled;
    }

    public int getPostStayDaysAfter() {
        return postStayDaysAfter;
    }

    public void setPostStayDaysAfter(int postStayDaysAfter) {
        this.postStayDaysAfter = postStayDaysAfter;
    }

    public String getPostStayReviewUrl() {
        return postStayReviewUrl;
    }

    public void setPostStayReviewUrl(String postStayReviewUrl) {
        this.postStayReviewUrl = postStayReviewUrl;
    }

    public boolean isWinBackEnabled() {
        return winBackEnabled;
    }

    public void setWinBackEnabled(boolean winBackEnabled) {
        this.winBackEnabled = winBackEnabled;
    }

    public int getWinBackMonthsSinceStay() {
        return winBackMonthsSinceStay;
    }

    public void setWinBackMonthsSinceStay(int winBackMonthsSinceStay) {
        this.winBackMonthsSinceStay = winBackMonthsSinceStay;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
