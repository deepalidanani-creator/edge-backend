package com.emeritus.edge_backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Speaker {
    
    @Id
    private Long id;
    private String name;

    // Required by JPA
    public Speaker() {}

    public Speaker(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
