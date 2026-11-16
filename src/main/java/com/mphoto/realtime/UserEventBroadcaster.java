package com.mphoto.realtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mphoto.user.dto.event.UserEvent;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

@ApplicationScoped
public class UserEventBroadcaster {
  @Inject
  Sse sse;
  private final ConcurrentHashMap<Long, Set<SseEventSink>> connections = new ConcurrentHashMap<>();

  public void add(Long userId, SseEventSink sink) {
    connections
        .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
        .add(sink);
    Log.infof("Added new connection for userId: %s, total connections: %s", userId, connections.get(userId).size());
  }

  public void send(Long userId, UserEvent userEvent) {
    var sinks = connections.get(userId);

    if (sinks == null)
      return;

    var event = sse.newEventBuilder()
        .name(userEvent.type().toString())
        .mediaType(MediaType.APPLICATION_JSON_TYPE)
        .data(userEvent.data())
        .build();

    for (var sink : sinks) {
      if (sink.isClosed()) {
        removeSink(sinks, sink, userId, null);
        continue;
      }
      try {
        Log.infof("Sending event %s to userId: %s", userEvent.type(), userId);
        sink.send(event)
            .exceptionally(ex -> {
              removeSink(sinks, sink, userId, ex);
              return null;
            });
      } catch (IllegalStateException ex) {
        removeSink(sinks, sink, userId, ex);
      }
    }
  }

  private void removeSink(
      Set<SseEventSink> sinks,
      SseEventSink sink,
      Long userId,
      Throwable ex) {
    Log.infof(ex, "Removing stale SSE sink for userId: %s", userId);
    sinks.remove(sink);
    if (sinks.isEmpty()) {
      connections.remove(userId);
      Log.infof("No more connections for userId: %s, removed from connections map", userId);
    }
    try {
      sink.close();
    } catch (Exception ignored) {
    }
  }

  public boolean isOnline(Long userId) {
    var sinks = connections.get(userId);
    return sinks != null && !sinks.isEmpty();
  }
}