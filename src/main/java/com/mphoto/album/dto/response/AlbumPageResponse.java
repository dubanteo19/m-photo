package com.mphoto.album.dto.response;

import java.util.List;

public record AlbumPageResponse(
        List<AlbumResponse> items) {
}