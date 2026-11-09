package com.mphoto.album.dto.response;

import java.time.Instant;

import com.mphoto.photo.dto.response.PhotoPageResponse;

public record AlbumDetailResponse(
        Long id,
        String name,
        Instant createdAt,
        PhotoPageResponse photos) {
}