package com.mphoto.model;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "m_users")
public class User extends PanacheEntity {

  @Column(unique = true, nullable = false)
  public String username;

  @Column(nullable = false)
  public String password;

  public String avatarUrl;

  public User() {
  }

  public User(Long id) {
    this.id = id;
  }

  public User(
      String username,
      String password,
      String avatarUrl) {
    this.username = username;
    this.password = password;
    this.avatarUrl = avatarUrl;
  }

  public static Uni<User> findByUsername(String username) {
    return find("username", username).firstResult();
  }

  public static Uni<User> findById(Long userId) {
    return find("id", userId).firstResult();
  }

  public void setAndHashPassword(String plainPassword) {
    this.password = BcryptUtil.bcryptHash(plainPassword);
  }

  public boolean checkPassword(String plainPassword) {
    return BcryptUtil.matches(plainPassword, this.password);
  }
}
