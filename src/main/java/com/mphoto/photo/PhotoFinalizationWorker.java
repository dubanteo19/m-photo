package com.mphoto.photo;

import com.mphoto.event.PhotoProcessedEvent;
import com.mphoto.photo.dto.UpdatePhotoCommand;
import com.mphoto.realtime.UserEventBroadcaster;
import com.mphoto.user.dto.event.UserEvent;
import com.mphoto.user.dto.event.UserEventType;

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
  UserEventBroadcaster userEventBroadcaster;
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
        .subscribe()
        .with(
            photoResponse -> {
              Log.infof("Photo %d finalized", photoResponse.id());
              userEventBroadcaster.send(event.userId(),
                  new UserEvent(UserEventType.PHOTO_PROCESSED, photoResponse));
            },
            failure -> Log.errorf(failure, "Failed to finalize photo %d", event.photoId())));
  }
}