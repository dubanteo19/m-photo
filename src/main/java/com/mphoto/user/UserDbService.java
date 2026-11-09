package com.mphoto.user;

import com.mphoto.model.User;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class UserDbService {

  @WithSession
  public Uni<User> findByUsername(String username) {
    return User.findByUsername(username)
        .onItem()
        .ifNull()
        .failWith(() -> new NotFoundException("User not found"));
  }

  @WithSession
  public Uni<User> findRequiredById(Long userId) {
    return User.findById(userId)
        .onItem()
        .ifNull()
        .failWith(() -> new NotFoundException("User not found"));
  }

  @WithSession
  public Uni<Long> findIdByUsername(String username) {
    return User.findByUsername(username)
        .onItem()
        .ifNull()
        .failWith(() -> new NotFoundException("User not found: " + username))
        .map(user -> user.id);
  }

}
