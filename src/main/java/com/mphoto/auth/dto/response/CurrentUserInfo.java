package com.mphoto.auth.dto.response;

import com.mphoto.model.User;
import com.mphoto.storage.StorageUtil;

public record CurrentUserInfo(
    Long id,
    String username,
    String avatarUrl) {

  public CurrentUserInfo(String username, String avatarUrl) {
    this(999999999L, username, avatarUrl);
  }

  public static CurrentUserInfo fromEntity(User user) {
    String avatarUrl = StorageUtil.getUrlPrefix() + user.avatarUrl;

    return new CurrentUserInfo(
        user.id,
        user.username,
        avatarUrl);

  }
}
