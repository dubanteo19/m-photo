package com.mphoto.photo.dto;

public record PhotoStorageProjection(
    Long photoCount,
    Long originalBytes,
    Long storedBytes) {
}