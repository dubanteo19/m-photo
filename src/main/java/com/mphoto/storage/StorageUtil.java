package com.mphoto.storage;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class StorageUtil {

  private static String urlPrefix;

  @ConfigProperty(name = "app.bucket")
  String bucketName;
  @ConfigProperty(name = "quarkus.minio.host")
  String minioHost;

  @ConfigProperty(name = "quarkus.minio.port")
  int minioPort;

  void init(@Observes StartupEvent ev) {
    urlPrefix = "http://" + minioHost + ":" + minioPort
    + "/" + bucketName + "/";

    // urlPrefix = "http://" + "cdn.dbt19.site" + "/" + bucketName + "/";
    Log.infof("Storage URL prefix initialized: %s", urlPrefix);
  }

  public static String getUrlPrefix() {
    if (urlPrefix == null) {
      throw new IllegalStateException("StorageUrlUtil not initialized");
    }
    return urlPrefix;
  }
}