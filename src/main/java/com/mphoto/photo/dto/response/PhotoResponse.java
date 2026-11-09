package com.mphoto.photo.dto.response;

import java.time.Instant;

import com.mphoto.model.Photo;
import com.mphoto.storage.StorageUtil;

public record PhotoResponse(
                Long id,
                Instant takenAt,
                Instant uploadedAt,
                Double latitude,
                Double longitude,
                Integer width,
                Integer height,
                Long size,
                String imageUrl,
                String thumbnailUrl) {
        public static PhotoResponse fromEntity(Photo photo) {
                String imageUrl = photo.objectKey != null ? StorageUtil.getUrlPrefix() + photo.objectKey : null;
                String thumbnailUrl = photo.thumbnailKey != null ? StorageUtil.getUrlPrefix() + photo.thumbnailKey
                                : null;
                return new PhotoResponse(
                                photo.id,
                                photo.takenAt,
                                photo.uploadedAt,
                                photo.latitude,
                                photo.longitude,
                                photo.width,
                                photo.height,
                                photo.sourceSize,
                                imageUrl,
                                thumbnailUrl);
        }
}