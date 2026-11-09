package com.mphoto.album.dto.response;

import java.time.Instant;

public record AlbumResponse(
        Long id,
        String name,
        Instant createdAt,
        Long photoCount,
        String coverUrl) {
}