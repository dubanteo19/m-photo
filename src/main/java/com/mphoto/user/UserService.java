package com.mphoto.user;

import com.mphoto.auth.dto.response.CurrentUserInfo;
import com.mphoto.common.cache.CacheConstants;
import com.mphoto.model.User;

import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserService {

  @Inject
  UserDbService userDbService;

  public Uni<User> findByUsername(String username) {
    return userDbService.findByUsername(username);
  }

  public Uni<CurrentUserInfo> getCurrentUserInfo(String username) {
    return userDbService.findIdByUsername(username)
        .chain(userId -> getCurrentUserInfoById(userId));
  }

  @CacheResult(cacheName = CacheConstants.CURRENT_USER_INFO_BY_ID)
  public Uni<CurrentUserInfo> getCurrentUserInfoById(Long userId) {
    Log.info("CACHE MISS! Fetching from DB layer for current user %d: " + userId);
    return userDbService.findRequiredById(userId).map(CurrentUserInfo::fromEntity);
  }

}
