package com.mphoto.user;

import org.eclipse.microprofile.jwt.Claim;

import com.mphoto.realtime.UserEventBroadcaster;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.SseEventSink;

@Path("/events")
@RequestScoped
public class UserEventResource {
  @Inject
  @Claim("userId")
  Long currentUserId;

  @Inject
  UserEventBroadcaster broadcaster;

  @GET
  @Produces(MediaType.SERVER_SENT_EVENTS)
  public void events(
      @Context SseEventSink sink) {
    broadcaster.add(currentUserId, sink);
  }
}