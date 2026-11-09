package com.mphoto.event;

public record PhotoUploadedEvent(
        Long photoId,
        String objectKey,
        String contentType) {
}