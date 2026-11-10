package com.mphoto.event;

import com.mphoto.photo.dto.PhotoMetadata;

public record PhotoProcessedEvent(
                Long photoId,
                String objectKey,
                String thumbnailKey,
                long size,
                PhotoMetadata metadata

) {
}