package com.mphoto.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PasswordService {
  @Inject Vertx vertx;

  public Uni<String> hashPassword(String plainPassword) {
    return Uni.createFrom()
        .item(() -> BcryptUtil.bcryptHash(plainPassword))
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
        .emitOn(
            runnable -> {
              if (Vertx.currentContext() != null) {
                Vertx.currentContext().runOnContext(v -> runnable.run());
              } else {
                vertx.runOnContext(v -> runnable.run());
              }
            });
  }
}
