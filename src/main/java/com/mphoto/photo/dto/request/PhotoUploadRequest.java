
package com.mphoto.photo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PhotoUploadRequest(
        @NotBlank @Size(max = 255) String filename,

        @NotBlank @Size(max = 100) String contentType,

        @NotNull @Positive Long size) {
}
