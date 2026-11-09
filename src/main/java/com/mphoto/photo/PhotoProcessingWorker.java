package com.mphoto.photo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.mphoto.event.PhotoUploadedEvent;
import com.mphoto.photo.dto.ProcessedImage;
import com.mphoto.storage.StorageService;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
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

    void onPhotoUploaded(@ObservesAsync PhotoUploadedEvent event) {
        Path source = null;
        Log.infof("Processing photo %d, object key: %s", event.photoId(), event.objectKey());
        try {

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
            ProcessedImage processed;
            // 2. Process the original image to AVIF format.
            try (var input = Files.newInputStream(source)) {
                processed = imageProcessor.process(input, event.contentType());
            }

            String outputKey = "photos/" + event.photoId() + ".avif";
            String thumbnailKey = "thumbnails/" + event.photoId() + ".avif";

            // 3. Upload the processed image to storage.
            storageService.uploadObject(outputKey, processed.bytes());

            Log.infof("Successfully processed photo %d", event.photoId());

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