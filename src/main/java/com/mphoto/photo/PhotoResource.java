package com.mphoto.photo;

import java.time.Instant;

import org.eclipse.microprofile.jwt.Claim;

import com.mphoto.event.PhotoUploadedEvent;
import com.mphoto.photo.dto.request.PhotoUploadRequest;
import com.mphoto.photo.dto.response.PhotoUploadResponse;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/photos")
@RequestScoped
public class PhotoResource {

  @Inject
  Event<PhotoUploadedEvent> eventPublisher;

  @Inject
  PhotoService photoService;

  @Inject
  @Claim("userId")
  Long currentUserId;

  @POST
  public Uni<PhotoUploadResponse> createUpload(
      PhotoUploadRequest request) {
    return photoService.createUpload(currentUserId, request);
  }

  @POST
  @Path("/{id}/complete")
  public Uni<Response> completeUpload(@PathParam("id") Long id) {
    return photoService.completeUpload(currentUserId, id)
        .invoke(updated -> {
          if (updated != null) {
            eventPublisher.fireAsync(
                new PhotoUploadedEvent(updated.id, updated.sourceObjectKey, updated.sourceMimeType));
          }
        })
        .replaceWith(Response.noContent().build());
  }

  @GET
  public Uni<Response> getPhotos(
      @QueryParam("cursor") Instant cursor) {
    return photoService.getPhotos(cursor).map(response -> Response.ok(response).build());
  }

  @GET
  @Path("/{id}")
  public Uni<Response> getPhoto(@PathParam("id") Long id) {
    return photoService.getPhoto(id).map(response -> Response.ok(response).build());
  }
  
  @DELETE
  @Path("/{id}")
  public Uni<Void> deletePhoto(@PathParam("id") Long id) {
    return photoService.deletePhoto(id);
  }
}
