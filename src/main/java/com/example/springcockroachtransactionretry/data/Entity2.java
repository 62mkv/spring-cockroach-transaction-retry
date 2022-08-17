package com.example.springcockroachtransactionretry.data;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table
@Data
@Builder
public class Entity2 {

    @Id
    private UUID id;

    private String name;

    private UUID parentId;

}
