package com.mphoto.photo;

import java.time.Instant;

import com.mphoto.model.Photo;
import com.mphoto.model.PhotoStatus;
import com.mphoto.model.User;
import com.mphoto.photo.dto.request.PhotoUploadRequest;
import com.mphoto.photo.dto.response.PhotoPageResponse;
import com.mphoto.photo.dto.response.PhotoResponse;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class PhotoDbService {

  @WithTransaction
  public Uni<Photo> createPhoto(Long userId, String objectKey, PhotoUploadRequest request) {
    Log.infof("Creating photo for user %s, objectKey %s, request %s", userId, objectKey, request);
    var photo = new Photo();
    photo.uploadedBy = new User(userId);
    photo.originalFilename = request.filename();
    photo.sourceMimeType = request.contentType();
    photo.sourceSize = request.size();
    photo.status = PhotoStatus.UPLOADING;
    photo.sourceObjectKey = objectKey;
    return photo.persistAndFlush();
  }

  @WithSession
  public Uni<PhotoPageResponse> getPhotos(Instant cursor) {

    String query = "status = ?1";
    Object[] params;

    if (cursor != null) {
      query += " and uploadedAt < ?2";
      params = new Object[] { PhotoStatus.READY, cursor };
    } else {
      params = new Object[] { PhotoStatus.READY };
    }

    return Photo.<Photo>find(query + " order by uploadedAt desc", params)
        .page(0, 30 + 1)
        .list()
        .map(photos -> {
          boolean hasNext = photos.size() > 30;
          if (hasNext)
            photos.remove(30);

          Instant nextCursor = hasNext
              ? photos.getLast().uploadedAt
              : null;

          return new PhotoPageResponse(
              photos.stream()
                  .map(PhotoResponse::fromEntity)
                  .toList(),
              nextCursor);
        });
  }

  @WithSession
  public Uni<Photo> getPhoto(Long id) {
    return Photo.find(
        "id = ?1 and status = ?2",
        id,
        PhotoStatus.READY).firstResult();
  }

  public Uni<Void> deletePhoto(Long id) {
    return findRequired(id)
        .flatMap(photo -> photo.delete())
        .replaceWithVoid();
  }

  @WithSession
  public Uni<Photo> findUpload(Long userId, Long photoId) {
    return Photo.find(
        "id = ?1 and uploadedBy.id = ?2",
        photoId,
        userId).firstResult();
  }

  public Uni<Photo> findRequired(Long photoId) {
    return Photo.<Photo>findById(photoId)
        .onItem().ifNull().failWith(
            () -> new NotFoundException("Photo not found"));
  }

  public Uni<Photo> markProcessing(Long photoId) {
    return findRequired(photoId)
        .flatMap(photo -> {
          photo.status = PhotoStatus.PROCESSING;
          return photo.persistAndFlush();
        });
  }
}
