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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rabbitmq.client.AMQP;
import crux.api.ICruxAPI;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.rabbitmq.RabbitMQClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static java.lang.String.format;

public class Handlers {
    private final static Logger logger = LoggerFactory.getLogger(Handlers.class.getName());
/*
    private final static ICruxAPI node;
    private final static IFn toJson;
    private final static ObjectMapper objectMapper = new ObjectMapper();

    static {
        Clojure.var("clojure.core", "require")
                .invoke(Clojure.read("jsonista.core"));

        final var nodeConfig = """
                {:crux.node/topology [crux.jdbc/topology]
                 :crux.jdbc/dbtype   "postgresql"
                 :crux.jdbc/dbname   "bob"
                 :crux.jdbc/host     "localhost"
                 :crux.jdbc/port     5432
                 :crux.jdbc/user     "bob"
                 :crux.jdbc/password "bob"}
                """;

        toJson = Clojure.var("jsonista.core", "write-value-as-string");
        node = Crux.startNode((Map<Keyword, ?>) datafy(nodeConfig));
        node.sync(Duration.ofSeconds(30)); // Become consistent for a max of 30s
    }

    public static Object datafy(String raw) {
        return Clojure.read(raw);
    }

    public static <T> T objectify(Object data, Class<T> cls) throws JsonProcessingException {
        return objectMapper.readValue((String) toJson.invoke(data), cls);
    }
*/

    private static void toJsonResponse(RoutingContext routingContext, Object content) {
        toJsonResponse(routingContext, content, 200);
    }

    private static void toJsonResponse(RoutingContext routingContext, Object content, int statusCode) {
        routingContext.response()
                .putHeader("Content-Type", "application/json")
                .setStatusCode(statusCode)
                .end(new JsonObject(Map.of("message", content)).encode());
    }

    private static void publishToEntities(RabbitMQClient queue, String type, JsonObject payload) {
        final var props = new AMQP.BasicProperties.Builder().type(type).build();

        queue.basicPublish("", "entities", props, payload.toBuffer(), it -> {
            if (it.succeeded())
                logger.debug(format("Published message with type %s on entities!", type));
            else
                logger.error(format("Error publishing message on entities: %s", it.cause().getMessage()));
        });
    }

    public static void healthCheckHandler(RoutingContext routingContext, RabbitMQClient queue) {
        // TODO use better health check
        final var foo = awaitEvent(h -> Main.node.status());
        Promise<HttpResponse<Buffer>> cruxCheck = Promise.promise();

        // TODO use better health check
        Promise<HttpResponse<Buffer>> rabbitCheck = Promise.promise();
        if (queue.isConnected()) {
            logger.debug("Health check succeeded for RabbitMQ!");
            cruxCheck.complete();
            rabbitCheck.complete();
        } else {
            logger.error("Health check failed for RabbitMQ!");
            cruxCheck.fail("Health check failed for Crux!");
            rabbitCheck.fail("Health check failed for RabbitMQ!");
        }

        CompositeFuture.all(rabbitCheck.future(), cruxCheck.future()).onSuccess(it -> {
            toJsonResponse(routingContext, "Yes we can! \uD83D\uDD28 \uD83D\uDD28");
        }).onFailure(it -> {
            toJsonResponse(routingContext, format("Failed Health Check: %s", it.getCause()), 503);
        });
    }

    public static void pipelineCreateHandler(RoutingContext routingContext, RabbitMQClient queue) {
        final var params = routingContext.request().params();
        final var group = params.get("group");
        final var name = params.get("name");
        final var pipeline = routingContext.getBodyAsJson();

        // TODO make JsonObject from params directly?!?

        final var payload = pipeline.put("name", name).put("group", group);

        publishToEntities(queue, "pipeline/create", payload);

        toJsonResponse(routingContext, format("Successfully Created Pipeline %s %s", group, name));
    }

    public static void pipelineDeleteHandler(RoutingContext routingContext, RabbitMQClient queue) {
        final var params = routingContext.request().params();
        final var group = params.get("group");
        final var name = params.get("name");
        final var payload = new JsonObject().put("name", name).put("group", group);

        publishToEntities(queue, "pipeline/delete", payload);

        toJsonResponse(routingContext, format("Successfully Deleted Pipeline %s %s", group, name));
    }

    public static void pipelineStartHandler(RoutingContext routingContext, RabbitMQClient queue) {
        final var params = routingContext.request().params();
        final var group = params.get("group");
        final var name = params.get("name");
        // TODO make JsonObject from params directly?!?
        final var payload = new JsonObject().put("name", name).put("group", group);

        publishToEntities(queue, "pipeline/start", payload);

        toJsonResponse(routingContext, format("Successfully Started Pipeline %s %s", group, name));
    }

    public static void pipelineStopHandler(RoutingContext routingContext, RabbitMQClient queue) {
        final var params = routingContext.request().params();
        final var group = params.get("group");
        final var name = params.get("name");
        final var number = params.get("number");
        final var payload = new JsonObject().put("name", name).put("group", group).put("number", number);

        publishToEntities(queue, "pipeline/stop", payload);

        toJsonResponse(routingContext, format("Successfully Stopped Pipeline %s %s %s", group, name, number));
    }

    public static void pipelineLogsHandler(RoutingContext routingContext) {
        final var params = routingContext.request().params();
        final var group = params.get("group");
        final var name = params.get("name");
        final var number = params.get("number");
        final var offset = params.get("offset");
        final var lines = params.get("lines");

        logger.info(group);
        logger.info(name);
        logger.info(number);
        logger.info(offset);
        logger.info(lines);
        // TODO DB interaction

        toJsonResponse(routingContext, format("Logs for Pipeline %s %s %s with Offset %s and Lines %s", group, name, number, offset, lines));
    }

    public static void pipelineStatusHandler(RoutingContext routingContext) {
        final var params = routingContext.request().params();
        final var group = params.get("group");
        final var name = params.get("name");
        final var number = params.get("number");

        logger.info(group);
        logger.info(name);
        logger.info(number);
        // TODO DB interaction

        toJsonResponse(routingContext, format("Status for Pipeline %s %s %s", group, name, number));
    }

    public static void pipelineArtifactHandler(RoutingContext routingContext, RabbitMQClient queue) {
        routingContext.response().sendFile("test.tar.gz");
        // TODO DB interaction and returning file via queue?

        toJsonResponse(routingContext, "Sending File completed!");
    }

    public static void pipelineListHandler(RoutingContext routingContext) {
        final var query = "{:find [e] :where [[e :type :pipeline]]}";

        toJsonResponse(routingContext, Main.node.db().query(query));
    }

    public static void resourceProviderCreateHandler(RoutingContext routingContext, RabbitMQClient queue) {
        final var name = routingContext.request().params().get("name");
        final var payload = routingContext.getBodyAsJson().put("name", name);

        logger.info(format("Creating Resource Provider with %s", payload));

        publishToEntities(queue, "resource-provider/create", payload);

        toJsonResponse(routingContext, format("Created Resource Provider %s", name));
    }

    public static void resourceProviderDeleteHandler(RoutingContext routingContext, RabbitMQClient queue) {
        final var name = routingContext.request().params().get("name");
        final var payload = new JsonObject().put("name", name);

        logger.info(format("Deleting Resource Provider with %s", payload));

        publishToEntities(queue, "resource-provider/delete", payload);

        toJsonResponse(routingContext, format("Deleted Resource Provider %s", name));
    }

    public static void resourceProviderListHandler(RoutingContext routingContext) {
        final var query = "{:find [e] :where [[e :type :resource-provider]]}";

        toJsonResponse(routingContext, Main.node.db().query(query));
    }

    public static void artifactStoreCreateHandler(RoutingContext routingContext, RabbitMQClient queue) {
        final var name = routingContext.request().params().get("name");
        final var payload = routingContext.getBodyAsJson().put("name", name);

        logger.info(format("Creating Artifact Store with %s", payload));

        publishToEntities(queue, "artifact-store/create", payload);

        toJsonResponse(routingContext, format("Created Artifact Store %s", name));
    }

    public static void artifactStoreDeleteHandler(RoutingContext routingContext, RabbitMQClient queue) {
        final var name = routingContext.request().params().get("name");
        final var payload = new JsonObject().put("name", name);

        logger.info(format("Deleting Artifact Store with %s", payload));

        publishToEntities(queue, "artifact-store/delete", payload);

        toJsonResponse(routingContext, format("Deleted Artifact Store %s", name));
    }

    public static void artifactStoreListHandler(RoutingContext routingContext) {
        final var query = "{:find [e] :where [[e :type :artifact-store]]}";

        toJsonResponse(routingContext, Main.node.db().query(query));
    }

    public static void apiSpecHandler(RoutingContext routingContext) {
        final var file = new File(Handlers.class.getResource("bob/api.yaml").getFile());

        try {
            routingContext.response()
                    .putHeader("Content-Type", "application/yaml")
                    .end(Files.readString(file.toPath()));

        } catch (IOException e) {
            final var msg = format("Could not read spec file: %s", e.getMessage());
            logger.error(msg);
            toJsonResponse(routingContext, msg, 500);
        }
    }
}
