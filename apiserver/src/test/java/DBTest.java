import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testing the Database interaction")
@ExtendWith(VertxExtension.class)
class DBTest {

    @DisplayName("➡️ A nested test with customized lifecycle")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    class CustomLifecycleTest {

        Vertx vertx;

        String apiSpec = "/bob/api.yaml";
        String httpHost = "localhost";
        Integer httpPort = 17777;

        RabbitMQOptions rabbitConfig = new RabbitMQOptions().setHost("localhost").setPort(5673);
        WebClientOptions clientConfig = new WebClientOptions().setDefaultHost("localhost").setDefaultPort(17777);

        @BeforeEach
        void prepare() {
            vertx = Vertx.vertx(new VertxOptions()
                    .setMaxEventLoopExecuteTime(1000)
                    .setPreferNativeTransport(true));
        }

        @Test
        @Order(1)
        @DisplayName("⬆️ Deploy APIServer")
        void deployAPIServer(VertxTestContext testContext) {
            final var queue = RabbitMQClient.create(vertx, rabbitConfig);

            vertx.deployVerticle(new APIServer(apiSpec, httpHost, httpPort, queue), testContext.succeeding(id ->
                    testContext.completeNow()));
        }

        @Test
        @Order(2)
        @DisplayName("List Pipelines")
        void listPipelines(VertxTestContext testContext) {
            final var queue = RabbitMQClient.create(vertx, rabbitConfig);
            final var client = WebClient.create(vertx, clientConfig);

            Checkpoint requestsServed = testContext.checkpoint();
            Checkpoint serverStarted = testContext.checkpoint();

            vertx.deployVerticle(new APIServer(apiSpec, httpHost, httpPort, queue), testContext.succeeding(id -> {
                serverStarted.flag();
                client.get("/pipelines").send(ar -> {
                    if (ar.failed()) {
                        testContext.failNow(ar.cause());
                    } else {
                        testContext.verify(() -> {
                            assertThat(ar.result().statusCode()).isEqualTo(200);
                            requestsServed.flag();
                        });
                    }
                });
            }));
        }
    }
}

// TODO PipelineLogs
// TODO PipelineStatus
// TODO PipelineArtifactFetch
// TODO ResourceProviderList
// TODO ArtifactStoreList


