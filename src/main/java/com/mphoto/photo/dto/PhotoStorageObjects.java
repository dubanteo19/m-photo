package com.mphoto.photo.dto;

import com.mphoto.model.Photo;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record PhotoStorageObjects(String sourceKey, String processedKey, String thumbnailKey) {
  public List<String> allKeys() {
    return Stream.of(sourceKey, processedKey, thumbnailKey).filter(Objects::nonNull).toList();
  }

  public static PhotoStorageObjects fromEntity(Photo photo) {
    return new PhotoStorageObjects(photo.sourceObjectKey, photo.objectKey, photo.thumbnailKey);
  }
}
