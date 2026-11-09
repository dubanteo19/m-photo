package com.mphoto.user;

import org.eclipse.microprofile.jwt.Claim;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/users")
@RequestScoped
public class UserResource {

  @Inject
  UserService userService;

  @Inject
  @Claim("userId")
  Long currentUserId;

}
