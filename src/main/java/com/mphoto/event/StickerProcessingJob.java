package com.mphoto.event;

public record StickerProcessingJob(
        Long stickerId,
        Long packageId,
        String objectKey,
        Long userId) {
}