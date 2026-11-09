package com.mphoto.photo.dto;

public record ProcessedImage(
        byte[] bytes,
        byte[] thumbnailBytes,
        int width,
        int height,
        long size) {
}