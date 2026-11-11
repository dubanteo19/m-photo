package com.mphoto.photo.dto;

public record AccountStorageStats(
    long photoCount,
    long originalBytes,
    long storedBytes,
    long savedBytes,
    double compressionPercentage) {
  public static AccountStorageStats from(PhotoStorageProjection projection) {
    long saved = projection.originalBytes() - projection.storedBytes();

    return new AccountStorageStats(
        projection.photoCount(),
        projection.originalBytes(),
        projection.storedBytes(),
        saved,
        projection.originalBytes() == 0
            ? 0
            : saved * 100.0 / projection.originalBytes());
  }
}