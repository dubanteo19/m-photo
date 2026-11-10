package com.mphoto.photo.dto;

import java.time.Instant;

public record UpdatePhotoCommand(
    long photoId,
    String objectKey,
    String thumbnailKey,
    long size,
    Instant takenAt,
    Double latitude,
    Double longitude,
    Integer width,
    Integer height) {
}