package com.mphoto.photo;

import java.time.Instant;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.mphoto.model.Photo;
import com.mphoto.model.PhotoStatus;
import com.mphoto.photo.dto.AccountStorageStats;
import com.mphoto.photo.dto.UpdatePhotoCommand;
import com.mphoto.photo.dto.request.PhotoUploadRequest;
import com.mphoto.photo.dto.response.PhotoPageResponse;
import com.mphoto.photo.dto.response.PhotoResponse;
import com.mphoto.photo.dto.response.PhotoUploadResponse;
import com.mphoto.storage.StorageService;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class PhotoService {
  @Inject
  PhotoDbService photoDbService;
  @Inject
  StorageService storageService;
  private static final Set<String> ALLOWED_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/heic",
      "image/heif",
      "image/webp",
      "image/avif");

  @ConfigProperty(name = "photo.upload.max-size", defaultValue = "52428800")
  long maxUploadSize;

  public Uni<PhotoUploadResponse> createUpload(Long userId, PhotoUploadRequest request) {
    validateUpload(request);
    var presigned = storageService.createPresignedUploadUrl(
        userId,
        request.filename(),
        "SOURCE_IMAGE");
    return photoDbService.createPhoto(
        userId,
        presigned.objectKey(),
        request).map(
            photo -> new PhotoUploadResponse(photo.id, presigned.uploadUrl()));

  }

  public Uni<Photo> completeUpload(Long userId, Long photoId) {
    return photoDbService.findUpload(userId, photoId)
        .onItem().ifNull().failWith(
            () -> new NotFoundException("Photo not found"))
        .flatMap(photo -> {
          if (photo.status != PhotoStatus.UPLOADING) {
            return Uni.createFrom().item(photo);
          }
          return photoDbService.markProcessing(photoId)
              .map(updated -> updated != null ? photo : null);
        });
  }

  public Uni<Void> finalizePhotoUpload(UpdatePhotoCommand command) {
    return photoDbService.update(command);
  }

  public Uni<PhotoPageResponse> getPhotos(Instant cursor) {
    return photoDbService.getPhotos(cursor);
  }

  public Uni<PhotoResponse> getPhoto(Long id) {
    return photoDbService.getPhoto(id)
        .onItem().ifNull().failWith(
            () -> new NotFoundException("Photo not found"))
        .map(PhotoResponse::fromEntity);
  }

  public Uni<Void> deletePhoto(Long id) {
    return photoDbService.deletePhoto(id);
  }

  private void validateUpload(PhotoUploadRequest request) {
    if (!ALLOWED_TYPES.contains(request.contentType())) {
      throw new BadRequestException(
          "Unsupported image content type");
    }

    if (request.size() > maxUploadSize) {
      throw new BadRequestException(
          "Image exceeds maximum upload size");
    }
  }

  public Uni<AccountStorageStats> getAccountStorageStats(Long userId) {
    return photoDbService.getStorageStats(userId)
        .map(AccountStorageStats::from);
  }
}
