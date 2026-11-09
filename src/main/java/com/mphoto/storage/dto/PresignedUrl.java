package com.mphoto.storage.dto;

public record PresignedUrl(String uploadUrl, String downloadUrl, String objectKey) {
}
