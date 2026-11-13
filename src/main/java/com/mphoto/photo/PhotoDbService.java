package com.mphoto.photo;

import java.time.Instant;
import java.util.List;

import com.mphoto.model.Photo;
import com.mphoto.model.PhotoStatus;
import com.mphoto.model.User;
import com.mphoto.photo.dto.PhotoStorageProjection;
import com.mphoto.photo.dto.UpdatePhotoCommand;
import com.mphoto.photo.dto.request.PhotoUploadRequest;
import com.mphoto.photo.dto.response.PhotoPageResponse;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class PhotoDbService {
  private static final int PAGE_SIZE = 30;

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
  public Uni<Void> update(UpdatePhotoCommand command) {
    return findRequired(command.photoId())
        .flatMap(photo -> {
          photo.objectKey = command.objectKey();
          photo.thumbnailKey = command.thumbnailKey();
          photo.size = command.size();
          photo.takenAt = command.takenAt();
          photo.latitude = command.latitude();
          photo.longitude = command.longitude();
          photo.width = command.width();
          photo.height = command.height();
          photo.status = PhotoStatus.READY;
          return photo.persistAndFlush().replaceWithVoid();
        });
  }

  @WithSession
  public Uni<PhotoPageResponse> getPhotos(Long userId, Instant cursor) {
    return findPhotos(userId, cursor, "deletedAt IS NULL", "uploadedAt");
  }

  @WithSession
  public Uni<PhotoPageResponse> getTrashedPhotos(Long userId, Instant cursor) {
    return findPhotos(userId, cursor, "deletedAt IS NOT NULL", "deletedAt");
  }

  private Uni<PhotoPageResponse> findPhotos(
      Long userId,
      Instant cursor,
      String condition,
      String sortField) {
    String query = """
        status = ?1
        AND uploadedBy.id = ?2
        AND %s
        AND (?3 IS NULL OR %s < ?3)
        ORDER BY %s DESC
        """.formatted(condition, sortField, sortField);

    return Photo.<Photo>find(query, PhotoStatus.READY, userId, cursor)
        .page(0, PAGE_SIZE + 1)
        .list()
        .map(PhotoPageResponse::toPageResponse);
  }

  @WithSession
  public Uni<Photo> getPhoto(Long id) {
    return Photo.find(
        "id = ?1 and status = ?2",
        id,
        PhotoStatus.READY).firstResult();
  }

  @WithTransaction
  public Uni<Void> moveToTrash(Long userId, List<Long> photoIds) {
    return Photo.update(
        """
            deletedAt = ?1
            where id in ?2
            and uploadedBy.id = ?3
            and deletedAt is null
            """,
        Instant.now(),
        photoIds,
        userId)
        .invoke(() -> Log.infof("User %d moved photos to trash: %d", userId, photoIds))
        .replaceWithVoid();
  }

  @WithTransaction
  public Uni<Void> restoreFromTrash(Long userId, List<Long> photoIds) {
    return Photo.update(
        """
            deletedAt = null
            where id in ?1
            and uploadedBy.id = ?2
            and deletedAt is not null
            """,
        photoIds,
        userId)
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

  public Uni<PhotoStorageProjection> getStorageStats(Long userId) {
    return Photo.find("""
        SELECT
            COUNT(p),
            COALESCE(SUM(p.sourceSize), 0),
            COALESCE(SUM(p.size), 0)
        FROM Photo p
        WHERE p.uploadedBy.id = ?1
          AND p.status = ?2
        """, userId, PhotoStatus.READY)
        .project(PhotoStorageProjection.class)
        .singleResult();
  }
}
