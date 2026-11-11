package com.mphoto.photo.dto;

public record ProcessedImage(
    byte[] bytes,
    byte[] thumbnailBytes,
    long size) {
}