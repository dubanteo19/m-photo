package com.mphoto.event;

public record StickerProcessedJob(Long stickerId, String outputKey, int width, int height, Long userId) {
}