package com.mphoto.photo;

import com.mphoto.event.PhotoProcessedEvent;
import com.mphoto.photo.dto.UpdatePhotoCommand;

import io.quarkus.logging.Log;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

@ApplicationScoped
public class PhotoFinalizationWorker {
  @Inject
  PhotoService photoService;

  @Inject
  Vertx vertx;

  public void finalizePhotoUpload(@ObservesAsync PhotoProcessedEvent event) {
    Log.infof("Finalizing photo upload for photo %d", event.photoId());

    var metadata = event.metadata();

    var command = new UpdatePhotoCommand(
        event.photoId(),
        event.objectKey(),
        event.thumbnailKey(),
        event.size(),
        metadata.takenAt(),
        metadata.latitude(),
        metadata.longitude(),
        metadata.width(),
        metadata.height());

    vertx.getOrCreateContext().runOnContext(ignored -> photoService.finalizePhotoUpload(command)
        .subscribe().with(
            result -> Log.infof("Photo %d finalized", event.photoId()),
            failure -> Log.errorf(failure, "Failed to finalize photo %d", event.photoId())));
  }
}