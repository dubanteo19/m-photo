package com.mphoto.auth.dto.response;

import com.mphoto.model.User;
import com.mphoto.storage.StorageUtil;

public record UserInfo(
    Long id,
    String username,
    String avatarUrl) {

  public static UserInfo fromEntity(User user) {
    String avatarUrl = user.avatarUrl != null ? StorageUtil.getUrlPrefix() + user.avatarUrl : null;

    return new UserInfo(
        user.id,
        user.username,
        avatarUrl);

  }
}
