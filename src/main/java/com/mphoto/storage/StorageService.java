package com.mphoto.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.mphoto.storage.dto.PresignedUrl;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.http.Method;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StorageService {

  private static final int UPLOAD_URL_EXPIRY_MINUTES = 10;

  @Inject
  MinioClient minioClient;

  @ConfigProperty(name = "app.bucket")
  String bucketName;

  @ConfigProperty(name = "quarkus.minio.host")
  String minioHost;

  @ConfigProperty(name = "quarkus.minio.port")
  String minioPort;

  public void uploadObject(String fileName, String objectKey) {
    try {
      Log.infof("Upload object %s, name %s ", objectKey, fileName);
      var uploadObjectArgs = UploadObjectArgs.builder()
          .bucket(bucketName)
          .object(objectKey)
          .filename(fileName)
          .contentType("application/zip")
          .build();
      minioClient.uploadObject(uploadObjectArgs);
    } catch (Exception e) {
      throw new StorageException("Could not upload object" + objectKey, e);
    }
  }

  public void uploadObject(String objectKey, byte[] bytes) {
    try {
      var putObjectArgs = PutObjectArgs.builder().bucket(bucketName).object(objectKey).stream(
          new ByteArrayInputStream(bytes), bytes.length, -1)
          .build();
      minioClient.putObject(putObjectArgs);
    } catch (Exception e) {
      throw new StorageException("Could not upload object" + objectKey, e);
    }
  }

  public InputStream downloadObject(String objectKey) {
    try {
      Log.infof("Download object %s", objectKey);
      return minioClient.getObject(
          GetObjectArgs.builder().bucket(bucketName).object(objectKey).build());
    } catch (Exception e) {
      throw new StorageException("Could not download object" + objectKey, e);
    }
  }

  public String getDownloadUrl(String objectKey, String fileName) {
    try {
      String presignedUrl = minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(bucketName)
              .object(objectKey)
              .extraQueryParams(
                  Map.of(
                      "response-content-disposition",
                      "attachment; filename=\"" + fileName + "\""))
              .expiry(UPLOAD_URL_EXPIRY_MINUTES, TimeUnit.MINUTES)
              .build());
      return presignedUrl;
    } catch (Exception e) {
      throw new StorageException("Could not generate download URL for object" + objectKey, e);
    }
  }

  public PresignedUrl createPresignedUploadUrl(String objectKey) {
    try {
      String uploadUrl = minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.PUT)
              .bucket(bucketName)
              .object(objectKey)
              .expiry(UPLOAD_URL_EXPIRY_MINUTES, TimeUnit.MINUTES)
              .build());

      String downloadUrl = StorageUtil.getUrlPrefix() + objectKey;

      return new PresignedUrl(uploadUrl, downloadUrl, objectKey);

    } catch (Exception e) {
      Log.error("Failed to generate presigned upload URL", e);
      throw new StorageException("Could not generate presigned URL", e.getCause());
    }
  }

  public PresignedUrl createPresignedUploadUrl(Long userId, String filename, String type) {

    String prefix = resolvePrefix(userId, type);
    String extension = extractExtension(filename);
    String objectKey = prefix + UUID.randomUUID() + extension;

    try {
      String uploadUrl = minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.PUT)
              .bucket(bucketName)
              .object(objectKey)
              .expiry(UPLOAD_URL_EXPIRY_MINUTES, TimeUnit.MINUTES)
              .build());

      String downloadUrl = StorageUtil.getUrlPrefix() + objectKey;

      return new PresignedUrl(uploadUrl, downloadUrl, objectKey);

    } catch (Exception e) {
      Log.error("Failed to generate presigned upload URL", e);
      throw new StorageException("Could not generate presigned URL", e.getCause());
    }
  }

  private String resolvePrefix(Long userId, String type) {
    if (type == null) {
      return "source/";
    }

    return switch (type) {
      case "AVATAR" -> "avatars/" + userId + "/";
      case "SOURCE_IMAGE" -> "source/";
      default -> "images/";
    };
  }

  private String extractExtension(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    return dotIndex >= 0 ? filename.substring(dotIndex) : "";
  }

  public static class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
