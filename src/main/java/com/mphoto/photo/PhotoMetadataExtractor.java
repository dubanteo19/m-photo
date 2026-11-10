package com.mphoto.photo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphoto.photo.dto.PhotoMetadata;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class PhotoMetadataExtractor {

    @Inject
    ObjectMapper objectMapper;

    private static final DateTimeFormatter EXIF_DATE = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    public PhotoMetadata extract(Path imagePath) throws IOException {
        var start = Instant.now();
        Process process = new ProcessBuilder(
                "exiftool",
                "-json",
                "-n",
                "-DateTimeOriginal",
                "-OffsetTimeOriginal",
                "-GPSLatitude",
                "-GPSLongitude",
                "-ImageWidth",
                "-ImageHeight",
                imagePath.toString())
                .redirectErrorStream(true)
                .start();

        try {
            String output = new String(process.getInputStream().readAllBytes());

            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("ExifTool timed out");
            }

            if (process.exitValue() != 0) {
                throw new IOException("ExifTool failed: " + output);
            }

            JsonNode root = objectMapper.readTree(output);
            JsonNode metadata = root.isArray() && !root.isEmpty()
                    ? root.get(0)
                    : objectMapper.createObjectNode();

            Log.infof("Metadata extraction took %d ms", Duration.between(start, Instant.now()).toMillis());
            return new PhotoMetadata(
                    parseTakenAt(metadata),
                    decimal(metadata, "GPSLatitude"),
                    decimal(metadata, "GPSLongitude"),
                    integer(metadata, "ImageWidth"),
                    integer(metadata, "ImageHeight"));

        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Metadata extraction interrupted", e);
        }
    }

    private Instant parseTakenAt(JsonNode metadata) {
        String date = text(metadata, "DateTimeOriginal");

        if (date == null) {
            return null;
        }

        var dateTime = LocalDateTime.parse(date, EXIF_DATE);
        String offset = text(metadata, "OffsetTimeOriginal");

        if (offset != null) {
            return dateTime.atOffset(
                    ZoneOffset.of(offset)).toInstant();
        }

        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Double decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asDouble();
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }
}