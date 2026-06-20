package com.ziyara.backend.infrastructure.messaging;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies a Kafka broker is reachable (Docker). Spring consumer/producer wiring is covered by
 * {@link StaffNotificationInboxProcessorTest} and local {@code @EmbeddedKafka} scenarios in development.
 */
@Tag("docker")
@Testcontainers
class StaffNotificationKafkaBrokerDockerTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.5"));

    @Test
    void canCreateTopicsOnBroker() throws ExecutionException, InterruptedException {
        String bootstrap = KAFKA.getBootstrapServers();
        try (AdminClient admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap))) {
            admin.createTopics(Collections.singleton(new NewTopic("ziyara.notifications.staff", 1, (short) 1)))
                    .all().get();
        }
        assertTrue(KAFKA.isRunning());
    }
}
