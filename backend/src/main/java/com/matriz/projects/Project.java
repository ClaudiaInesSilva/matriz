package com.matriz.projects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {
    @Id
    @Column(name = "project_id", nullable = false)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    protected Project() {}

    public Project(UUID id, String name, String description, UUID ownerId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void updateDetails(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
