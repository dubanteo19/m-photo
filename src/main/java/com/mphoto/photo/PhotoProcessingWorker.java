package com.mphoto.photo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;

import com.mphoto.event.PhotoProcessedEvent;
import com.mphoto.event.PhotoUploadedEvent;
import com.mphoto.photo.dto.ProcessedImage;
import com.mphoto.storage.StorageService;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

@ApplicationScoped
public class PhotoProcessingWorker {
  @Inject
  StorageService storageService;
  @Inject
  ImageProcessor imageProcessor;
  @Inject
  PhotoService photoService;
  @Inject
  PhotoMetadataExtractor metadataExtractor;
  @Inject
  Event<PhotoProcessedEvent> photoProcessedEvent;

  void onPhotoUploaded(@ObservesAsync PhotoUploadedEvent event) {
    Path source = null;
    Log.infof("Processing photo %d, object key: %s", event.photoId(), event.objectKey());
    try {
      var start = Instant.now();
      source = Files.createTempFile("photo-", ".source");
      // Download the original image from storage to a temporary file.
      try (var input = storageService.downloadObject(event.objectKey())) {
        Files.copy(
            input,
            source,
            StandardCopyOption.REPLACE_EXISTING);
      }
      // 1. Extract metadata from the original.
      var metadata = metadataExtractor.extract(source);
      Log.infof("Extracted metadata for photo %d: %s", event.photoId(), metadata);
      ProcessedImage processed;
      // 2. Process the original image to AVIF format.
      try (var input = Files.newInputStream(source)) {
        processed = imageProcessor.process(input, event.contentType());
      }

      String photoKey = "photos/" + event.photoId() + ".avif";
      String thumbnailKey = "thumbnails/" + event.photoId() + ".avif";

      // 3. Upload the processed image to storage.
      storageService.uploadObject(photoKey, processed.bytes());

      // 4. Upload the thumbnail to storage.
      storageService.uploadObject(thumbnailKey, processed.thumbnailBytes());

      Log.infof("Successfully processed photo %d, took %d ms", event.photoId(),
          Duration.between(start, Instant.now()).toMillis());

      // 5. Fire an event to notify that the photo has been processed.
      photoProcessedEvent.fireAsync(
          new PhotoProcessedEvent(
              event.photoId(),
              photoKey,
              thumbnailKey,
              processed.size(),
              metadata));
    } catch (Exception e) {
      Log.errorf(e, "Failed to process photo %d", event.photoId());
    } finally {
      if (source != null) {
        try {
          Files.deleteIfExists(source);
        } catch (Exception e) {
          Log.warnf(e, "Failed to delete temporary file %s", source);
        }
      }
    }
  }
}