package com.mphoto.photo;

import com.mphoto.photo.dto.PhotoStorageObjects;
import com.mphoto.storage.StorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class StorageCleanupWorker {
  @Inject StorageService storageService;

  void onPhotoUploaded(@ObservesAsync List<PhotoStorageObjects> photoStorageObjects) {
    var objectKeys =
        photoStorageObjects.stream().flatMap(objects -> objects.allKeys().stream()).toList();
    if (objectKeys.isEmpty()) return;
    try {
      storageService.deleteObjects(objectKeys);
    } catch (Exception e) {
    }
  }
}
