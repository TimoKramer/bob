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

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Keyword;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import crux.api.Crux;
import crux.api.ICruxAPI;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;

import static java.lang.String.format;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class.getName());
    private final static ObjectMapper objectMapper = new ObjectMapper();
    protected final static ICruxAPI node;
    protected final static IFn toJson;

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

    public static void main(String[] args) {
        final var vertx = Vertx.vertx(new VertxOptions().setHAEnabled(true));

        ConfigRetriever.create(vertx).getConfig(config -> {
            if (config.succeeded()) {
                final var conf = config.result();
                final var rmqConfig = conf.getJsonObject("rabbitmq");
                final var httpConfig = conf.getJsonObject("http");

                final var apiSpec = httpConfig.getString("apiSpec", "/bob/api.yaml");
                final var httpHost = httpConfig.getString("host", "localhost");
                final var httpPort = httpConfig.getInteger("port", 7777);

                final var queue = RabbitMQClient.create(vertx, new RabbitMQOptions(rmqConfig));

                vertx.deployVerticle(new APIServer(apiSpec, httpHost, httpPort, queue), v -> {
                    if (v.succeeded())
                        logger.info(format("Deployed on verticle: %s", v.result()));
                    else
                        logger.error(format("Deployment error: %s", v.cause()));
                });
            }
        });
    }
}
