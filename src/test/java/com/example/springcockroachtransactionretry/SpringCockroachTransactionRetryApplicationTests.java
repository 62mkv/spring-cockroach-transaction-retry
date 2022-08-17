package com.example.springcockroachtransactionretry;

import com.example.springcockroachtransactionretry.data.Entity1;
import com.example.springcockroachtransactionretry.repository.Entity1Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

@SpringBootTest
@Testcontainers
class SpringCockroachTransactionRetryApplicationTests {

	private static final Logger log = LoggerFactory.getLogger("main");
	private static final DockerImageName cockroachImage = DockerImageName.parse("cockroachdb/cockroach:v22.1.5");
	private static final UUID ID = UUID.fromString("1f5dd47d-2dc8-43bd-b454-168d8670eca7");

	@Container
	private static GenericContainer<?> cockroach = new GenericContainer<>(cockroachImage)
			.withExposedPorts(26257)
			.withLogConsumer(new Slf4jLogConsumer(log))
			.withCommand("start-single-node --insecure");

	@DynamicPropertySource
	static void exposeContainerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://root@localhost:%d/defaultdb", cockroach.getMappedPort(26257)));
		registry.add("spring.liquibase.url", () -> String.format("jdbc:postgresql://localhost:%d/defaultdb", cockroach.getMappedPort(26257)));
	}

	@Autowired
	private Entity1Repository repository;

	@Autowired
	private ReactiveTransactionManager transactionManager;
	private TransactionalOperator rxtx;

	@BeforeEach
	void setUp() {
		this.rxtx = TransactionalOperator.create(this.transactionManager);
	}

	@Test
	void testTransactionRetries() {
		var entity1 = Entity1.builder().id(ID).name("name1").build();
		var entity2 =  Entity1.builder().id(ID).name("name2").build();
		var flow = rxtx.transactional(repository.saveAll(List.of(entity1, entity2)));
		StepVerifier.create(flow)
				.expectNext(entity1)
				.verifyErrorMatches(DataIntegrityViolationException.class::isInstance);
	}

}
