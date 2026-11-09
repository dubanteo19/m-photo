package com.mphoto.auth;

import com.mphoto.auth.dto.request.UserLoginRequest;
import com.mphoto.auth.dto.request.UserRegisterRequest;
import com.mphoto.auth.dto.response.UserInfo;
import com.mphoto.exception.InvalidCredentialsException;
import com.mphoto.exception.UsernameAlreadyExistsException;
import com.mphoto.model.User;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthService {

  @WithTransaction
  public Uni<UserInfo> registerUser(UserRegisterRequest request) {
    return User.findByUsername(request.username())
        .chain(
            existingUser -> {
              if (existingUser != null) {
                return Uni.createFrom()
                    .failure(new UsernameAlreadyExistsException(request.username()));
              }
              var user = new User();
              user.username = request.username();
              user.password = request.password();
              return user.persist().replaceWith(() -> UserInfo.fromEntity(user));
            });
  }

  @WithTransaction
  public Uni<UserInfo> loginUser(UserLoginRequest request) {
    return User.findByUsername(request.username())
        .map(
            user -> {
              if (user == null || !user.checkPassword(request.password())) {
                throw new InvalidCredentialsException();
              }
              return UserInfo.fromEntity(user);
            });
  }
}
