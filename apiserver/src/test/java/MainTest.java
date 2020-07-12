/*
 * This file is part of Bob.
 *
 * Bob is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bob is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bob. If not, see <http://www.gnu.org/licenses/>.
 */

import io.vertx.config.ConfigRetriever;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("👋 A fairly basic test example")
@ExtendWith(VertxExtension.class)
class APIServerTest {

    @Test
    @DisplayName("🚀 Deploy a HTTP service verticle and make 10 requests")
    void useAPIServer(Vertx vertx, VertxTestContext testContext) {
        Checkpoint deploymentCheckpoint = testContext.checkpoint();
        Checkpoint requestCheckpoint = testContext.checkpoint(10);
        final var apiSpec = "/bob/api.yaml";
        final var httpHost = "localhost";
        final var httpPort = 17777;

        final var rabbitConfig = new RabbitMQOptions().setHost("localhost").setPort(5673);
        final var queue = RabbitMQClient.create(vertx, rabbitConfig);

        final var cruxConfig = new WebClientOptions().setDefaultHost("localhost").setDefaultPort(7779);
        final var crux = WebClient.create(vertx, cruxConfig);

        final var clientConfig = new WebClientOptions().setDefaultHost("localhost").setDefaultPort(17777);
        final var client = WebClient.create(vertx, clientConfig);

        vertx.deployVerticle(new APIServer(apiSpec, httpHost, httpPort, queue, crux), testContext.succeeding(id -> {
            deploymentCheckpoint.flag();
            for (int i = 0; i < 10; i++) {
                client.get("/can-we-build-it")
                        .as(BodyCodec.string())
                        .send(testContext.succeeding(resp -> {
                            testContext.verify(() -> {
                                assertThat(resp.statusCode()).isEqualTo(200);
                                assertThat(resp.body()).contains("Yes we can!");
                                requestCheckpoint.flag();
                            });
                        }));
            }
        }));
    }

    @DisplayName("➡️ A nested test with customized lifecycle")
    @Nested
    class CustomLifecycleTest {

        Vertx vertx;

        @BeforeEach
        void prepare() {
            vertx = Vertx.vertx(new VertxOptions()
                    .setMaxEventLoopExecuteTime(1000)
                    .setPreferNativeTransport(true));
        }

        @Test
        @DisplayName("⬆️ Deploy APIServer")
        void deployAPIServer(VertxTestContext testContext) {
            final var apiSpec = "/bob/api.yaml";
            final var httpHost = "localhost";
            final var httpPort = 17777;

            final var rabbitConfig = new RabbitMQOptions().setHost("localhost").setPort(5673);
            final var queue = RabbitMQClient.create(vertx, rabbitConfig);

            final var cruxConfig = new WebClientOptions().setDefaultHost("localhost").setDefaultPort(7779);
            final var crux = WebClient.create(vertx, cruxConfig);

            final var clientConfig = new WebClientOptions().setDefaultHost("localhost").setDefaultPort(17777);
            final var client = WebClient.create(vertx, clientConfig);

            vertx.deployVerticle(new APIServer(apiSpec, httpHost, httpPort, queue, crux), testContext.succeeding(id ->
                    testContext.completeNow()));
        }

        @Test
        @DisplayName("🛂 Fetch api.yaml from APIServer")
        void httpRequest(VertxTestContext testContext) {
            final var apiSpec = "/bob/api.yaml";
            final var httpHost = "localhost";
            final var httpPort = 17777;

            final var rabbitConfig = new RabbitMQOptions().setHost("localhost").setPort(5673);
            final var queue = RabbitMQClient.create(vertx, rabbitConfig);

            final var cruxConfig = new WebClientOptions().setDefaultHost("localhost").setDefaultPort(7779);
            final var crux = WebClient.create(vertx, cruxConfig);

            final var clientConfig = new WebClientOptions().setDefaultHost("localhost").setDefaultPort(17777);
            final var client = WebClient.create(vertx, clientConfig);

            vertx.deployVerticle(new APIServer(apiSpec, httpHost, httpPort, queue, crux), testContext.succeeding(id -> {
                client.get("/api.yaml")
                        .as(BodyCodec.string())
                        .send(testContext.succeeding(resp -> {
                            testContext.verify(() -> {
                                assertThat(resp.statusCode()).isEqualTo(200);
                                assertThat(resp.body()).contains("title: Bob the Builder");
                                testContext.completeNow();
                            });
                        }));
            }));
        }

        @Test
        @DisplayName("Create Test Pipeline")
        void createPipeline(VertxTestContext testContext) {
            final var apiSpec = "/bob/api.yaml";
            final var httpHost = "localhost";
            final var httpPort = 17777;

            final var rabbitConfig = new RabbitMQOptions().setHost("localhost").setPort(5673);
            final var queue = RabbitMQClient.create(vertx, rabbitConfig);

            final var cruxConfig = new WebClientOptions().setDefaultHost("localhost").setDefaultPort(7779);
            final var crux = WebClient.create(vertx, cruxConfig);

            final var clientConfig = new WebClientOptions().setDefaultHost("localhost").setDefaultPort(17777);
            final var client = WebClient.create(vertx, clientConfig);

            vertx.deployVerticle(new APIServer(apiSpec, httpHost, httpPort, queue, crux), testContext.succeeding(id -> {
                client.get("/api.yaml")
                        .as(BodyCodec.string())
                        .send(testContext.succeeding(resp -> {
                            testContext.verify(() -> {
                                assertThat(resp.statusCode()).isEqualTo(200);
                                assertThat(resp.body()).contains("title: Bob the Builder");
                                testContext.completeNow();
                            });
                        }));
            }));
        }

        @AfterEach
        void cleanup() {
            vertx.close();
        }
    }
}
