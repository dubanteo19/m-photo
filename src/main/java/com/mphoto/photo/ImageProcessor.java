
package com.mphoto.photo;

import jakarta.enterprise.context.ApplicationScoped;

import com.mphoto.photo.dto.ProcessedImage;

import io.quarkus.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ImageProcessor {

  private static final int AVIF_SPEED = 6;
  private static final String AVIF_MIN = "20";
  private static final String AVIF_MAX = "30";

  private static final long PROCESS_TIMEOUT_SECONDS = 120;

  public ProcessedImage process(InputStream imageStream, String contentType) throws IOException {
    String extension = getExtensionFromContentType(contentType);
    Path workDir = Files.createTempDirectory("photo-");
    try {
      Path input = workDir.resolve("source" + extension);
      Path mainJpeg = workDir.resolve("main.jpg");
      Path thumbnailJpeg = workDir.resolve("thumbnail.jpg");

      Path mainAvif = workDir.resolve("main.avif");
      Path thumbnailAvif = workDir.resolve("thumbnail.avif");

      Path log = workDir.resolve("process.log");

      Files.copy(
          imageStream,
          input,
          StandardCopyOption.REPLACE_EXISTING);

      // Resize main image, preserving aspect ratio and orientation.
      runCommand(List.of(
          "vipsthumbnail",
          input.toString(),
          "--size", "2000x2000",
          "--output", mainJpeg.toString()), log);
      // Generate thumbnail from the original, not the main image.
      runCommand(List.of(
          "vipsthumbnail",
          input.toString(),
          "--size", "200x200",
          "--output", thumbnailJpeg.toString()), log);

      encodeAvif(mainJpeg, mainAvif, log);
      encodeAvif(thumbnailJpeg, thumbnailAvif, log);

      byte[] mainBytes = Files.readAllBytes(mainAvif);
      byte[] thumbnailBytes = Files.readAllBytes(thumbnailAvif);

      return new ProcessedImage(
          mainBytes,
          thumbnailBytes,
          mainBytes.length);
    } finally {
      try (var paths = Files.walk(workDir)) {
        paths.sorted(Comparator.reverseOrder())
            .forEach(path -> {
              try {
                Files.deleteIfExists(path);
              } catch (IOException e) {
                Log.warnf(e, "Failed to delete temporary file %s", path);
              }
            });
      }
    }

  }

  private void encodeAvif(
      Path input,
      Path output,
      Path log) throws IOException {
    var start = Instant.now();
    runCommand(List.of(
        "avifenc",
        "--min", AVIF_MIN,
        "--max", AVIF_MAX,
        "--speed", String.valueOf(AVIF_SPEED),
        input.toString(),
        output.toString()), log);
    Log.infof("AVIF encoding %s took %d ms", output.getFileName(),
        Duration.between(start, Instant.now()).toMillis());
  }

  private void runCommand(
      List<String> command,
      Path log) throws IOException {

    Process process = new ProcessBuilder(command)
        .redirectErrorStream(true)
        .redirectOutput(log.toFile())
        .start();

    try {
      boolean completed = process.waitFor(
          PROCESS_TIMEOUT_SECONDS,
          TimeUnit.SECONDS);

      if (!completed) {
        process.destroyForcibly();
        throw new IOException(
            "Process timed out: " + command.get(0));
      }

      if (process.exitValue() != 0) {
        throw new IOException(
            command.get(0) + " failed: " + Files.readString(log));
      }

    } catch (InterruptedException e) {
      process.destroyForcibly();
      Thread.currentThread().interrupt();
      throw new IOException("Process interrupted: " + command.get(0), e);
    }
  }

  private String getExtensionFromContentType(String contentType) {
    return switch (contentType) {
      case "image/jpeg", "image/jpg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/heic" -> ".heic";
      case "image/heif" -> ".heif";
      default -> throw new IllegalArgumentException(
          "Unsupported image type: " + contentType);
    };
  }
}
