package com.mphoto.auth;

import com.mphoto.auth.dto.request.UserLoginRequest;
import com.mphoto.auth.dto.request.UserRegisterRequest;
import com.mphoto.user.UserService;

import io.quarkus.logging.Log;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/auth")
@RequestScoped
public class AuthResource {

  @Inject
  AuthService authService;
  @Inject
  PasswordService passwordService;
  @Inject
  UserService userService;
  @Inject
  JsonWebToken jwt;

  @ConfigProperty(name = "app.domain")
  Optional<String> host;

  @POST
  @Path("/register")
  public Uni<Response> register(UserRegisterRequest request) {
    return passwordService
        .hashPassword(request.password())
        .chain(
            hashedPassword -> {
              var secureRequest = new UserRegisterRequest(
                  request.username(), hashedPassword);
              return authService
                  .registerUser(secureRequest)
                  .map(
                      userInfo -> Response.status(Response.Status.CREATED).entity(userInfo).build());
            });
  }

  @GET
  @Path("/me")
  public Uni<Response> me() {
    String username = jwt.getName();
    if (username == null) {
      Log.warn("No valid session found.");
      return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).build());
    }

    return userService
        .getCurrentUserInfo(username)
        .map(
            userInfo -> {
              if (userInfo == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
              }
              Log.infof("User info retrieved for username: %s", username);
              return Response.ok(userInfo).build();
            });
  }

  @POST
  @Path("/login")
  public Uni<Response> login(UserLoginRequest request) {
    return authService
        .loginUser(request)
        .map(
            userInfo -> {
              String signedToken = Jwt.issuer("https://dbt19.site")
                  .upn(userInfo.username())
                  .subject(String.valueOf(userInfo.id()))
                  .claim("userId", userInfo.id())
                  .groups("USER")
                  .expiresIn(Duration.ofDays(30))
                  .sign();

              var builder = new NewCookie.Builder("m_user")
                  .value(signedToken)
                  .path("/")
                  .maxAge((int) Duration.ofDays(30).toSeconds())
                  .secure(false)
                  .httpOnly(true)
                  .sameSite(NewCookie.SameSite.LAX);

              if (host.isPresent()) {
                builder.domain(host.get());
              }

              var authCookie = builder.build();

              return Response.ok(userInfo).cookie(authCookie).build();
            });
  }

  @POST
  @Path("/logout")
  public Uni<Response> logout() {
    var builder = new NewCookie.Builder("m_user")
        .path("/")
        .maxAge(0)
        .secure(false)
        .httpOnly(true)
        .sameSite(NewCookie.SameSite.LAX);

    if (host.isPresent()) {
      builder.domain(host.get());
    }
    var authCookie = builder.build();
    return Uni.createFrom().item(() -> Response.ok().cookie(authCookie).build());
  }
}
