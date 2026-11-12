package com.mphoto.photo.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PhotoIdsRequest(
    @NotEmpty List<@NotNull Long> ids) {
}