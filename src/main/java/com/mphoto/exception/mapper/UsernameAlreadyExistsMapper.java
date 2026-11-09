package com.mphoto.exception.mapper;

import com.mphoto.exception.UsernameAlreadyExistsException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UsernameAlreadyExistsMapper
    implements ExceptionMapper<UsernameAlreadyExistsException> {

  @Override
  public Response toResponse(UsernameAlreadyExistsException exception) {
    return Response.status(Response.Status.CONFLICT)
        .entity(new ErrorResponsePayload(exception.getMessage()))
        .build();
  }

  public record ErrorResponsePayload(String error) {}
}
