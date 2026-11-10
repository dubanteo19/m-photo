package com.mphoto.photo.dto.response;

import java.time.Instant;
import java.util.List;

public record PhotoPageResponse(
    List<PhotoResponse> items,
    Instant nextCursor) {
}