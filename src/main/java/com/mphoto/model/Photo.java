package com.mphoto.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "photos")
public class Photo extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    public User uploadedBy;

    public Instant takenAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    public Instant uploadedAt;

    public Double latitude;
    public Double longitude;

    public Integer width;
    public Integer height;

    public Long sourceSize;
    public Long size;

    public String sourceObjectKey;
    public String objectKey;
    public String thumbnailKey;

    public String originalFilename;
    public String sourceMimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public PhotoStatus status = PhotoStatus.UPLOADING;
}
