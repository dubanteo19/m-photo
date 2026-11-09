package com.mphoto.user;

import org.eclipse.microprofile.jwt.Claim;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/events")
@RequestScoped
public class UserEventResource {

    @Inject
    @Claim("userId")
    Long currentUserId;

}