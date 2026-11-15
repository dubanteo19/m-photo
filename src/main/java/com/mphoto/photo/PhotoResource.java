package com.mphoto.photo;

import com.mphoto.event.PhotoUploadedEvent;
import com.mphoto.photo.dto.PhotoStorageObjects;
import com.mphoto.photo.dto.request.PhotoIdsRequest;
import com.mphoto.photo.dto.request.PhotoUploadRequest;
import com.mphoto.photo.dto.response.PhotoUploadResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import org.eclipse.microprofile.jwt.Claim;

@Path("/photos")
@RequestScoped
public class PhotoResource {

  @Inject Event<PhotoUploadedEvent> eventPublisher;
  @Inject Event<List<PhotoStorageObjects>> photoPurgedEventPubliser;

  @Inject PhotoService photoService;

  @Inject
  @Claim("userId")
  Long currentUserId;

  @POST
  public Uni<PhotoUploadResponse> createUpload(@Valid PhotoUploadRequest request) {
    return photoService.createUpload(currentUserId, request);
  }

  @POST
  @Path("/{id}/complete")
  public Uni<Response> completeUpload(@PathParam("id") Long id) {
    return photoService
        .completeUpload(currentUserId, id)
        .invoke(
            updated -> {
              if (updated != null) {
                eventPublisher.fireAsync(
                    new PhotoUploadedEvent(
                        updated.id, updated.sourceObjectKey, updated.sourceMimeType));
              }
            })
        .replaceWith(Response.noContent().build());
  }

  @GET
  public Uni<Response> getPhotos(@QueryParam("cursor") Instant cursor) {
    return photoService
        .getPhotos(currentUserId, cursor)
        .map(response -> Response.ok(response).build());
  }

  @GET
  @Path("/trash")
  public Uni<Response> getTrashedPhotos(@QueryParam("cursor") Instant cursor) {
    return photoService
        .getTrashedPhotos(currentUserId, cursor)
        .map(response -> Response.ok(response).build());
  }

  @GET
  @Path("/{id}")
  public Uni<Response> getPhoto(@PathParam("id") Long id) {
    return photoService.getPhoto(id).map(response -> Response.ok(response).build());
  }

  @PATCH
  @Path("/trash")
  public Uni<Void> moveToTrash(@Valid PhotoIdsRequest request) {
    return photoService.moveToTrash(currentUserId, request.ids());
  }

  @POST
  @Path("/trash/purge")
  public Uni<Response> purgePhotos(@Valid PhotoIdsRequest request) {
    return photoService
        .purge(currentUserId, request.ids())
        .invoke(photoPurgedEventPubliser::fireAsync)
        .replaceWith(Response.noContent().build());
  }

  @PATCH
  @Path("/restore")
  public Uni<Void> restoreFromTrash(@Valid PhotoIdsRequest request) {
    return photoService.restoreFromTrash(currentUserId, request.ids());
  }

  @GET
  @Path("/storage-stats")
  public Uni<Response> getStorageStats() {
    return photoService
        .getAccountStorageStats(currentUserId)
        .map(stats -> Response.ok(stats).build());
  }
}
