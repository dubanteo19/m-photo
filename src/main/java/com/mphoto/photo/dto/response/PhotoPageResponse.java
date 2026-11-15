package com.mphoto.photo.dto.response;

import com.mphoto.model.Photo;
import java.time.Instant;
import java.util.List;

public record PhotoPageResponse(List<PhotoResponse> items, Instant nextCursor) {
  public static PhotoPageResponse toPageResponse(List<Photo> photos) {
    boolean hasNext = photos.size() > 30;
    if (hasNext) photos.remove(30);

    Instant nextCursor = hasNext ? photos.getLast().uploadedAt : null;

    return new PhotoPageResponse(
        photos.stream().map(PhotoResponse::fromEntity).toList(), nextCursor);
  }
}

