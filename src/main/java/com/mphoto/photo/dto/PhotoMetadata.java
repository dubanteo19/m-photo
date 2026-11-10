package com.mphoto.photo.dto;

import java.time.Instant;

public record PhotoMetadata(
    Instant takenAt,
    Double latitude,
    Double longitude,
    Integer width,
    Integer height) {
}