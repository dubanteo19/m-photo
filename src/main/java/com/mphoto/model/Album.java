package com.mphoto.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "albums")
public class Album extends PanacheEntity {

    @Column(nullable = false, length = 100)
    public String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    public User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "album_photos", joinColumns = @JoinColumn(name = "album_id"), inverseJoinColumns = @JoinColumn(name = "photo_id"), uniqueConstraints = @UniqueConstraint(columnNames = {
            "album_id", "photo_id" }))
    public Set<Photo> photos = new HashSet<>();
}
