package com.suttori.entity;

public class ReportDataRequest {
    private Long userId;
    private Long stickerSetId;
    private String data;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStickerSetId() {
        return stickerSetId;
    }

    public void setStickerSetId(Long stickerSetId) {
        this.stickerSetId = stickerSetId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
