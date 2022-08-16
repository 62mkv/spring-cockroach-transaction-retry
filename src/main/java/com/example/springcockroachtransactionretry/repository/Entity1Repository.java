package com.example.springcockroachtransactionretry.repository;

import com.example.springcockroachtransactionretry.data.Entity1;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface Entity1Repository extends R2dbcRepository<Entity1, UUID> {
}
