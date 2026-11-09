package com.mphoto.exception.mapper;

import com.mphoto.exception.InvalidCredentialsException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidCredentialsMapper implements ExceptionMapper<InvalidCredentialsException> {

  @Override
  public Response toResponse(InvalidCredentialsException exception) {
    return Response.status(Response.Status.UNAUTHORIZED)
        .entity(new UsernameAlreadyExistsMapper.ErrorResponsePayload(exception.getMessage()))
        .build();
  }
}
