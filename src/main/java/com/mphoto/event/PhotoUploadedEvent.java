package com.mphoto.event;

public record PhotoUploadedEvent(Long userId, Long photoId, String objectKey, String contentType) {
}
