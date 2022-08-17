package com.example.springcockroachtransactionretry.data;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table
@Data
@Builder
public class Entity1 implements Persistable<UUID> {
    @Id
    UUID id;

    String name;

    @Override
    public boolean isNew() {
        return true;
    }
}
