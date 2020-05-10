import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

fun toJsonResponse(routingContext: RoutingContext, content: Any): Future<Void> =
    routingContext.response()
        .putHeader("content-type", "application/json")
        .end(JsonObject(mapOf("message" to content)).encode())

fun healthCheckHandler(routingContext: RoutingContext) =
    toJsonResponse(routingContext, "Yes we can! \uD83D\uDD28 \uD83D\uDD28")

fun pipelineCreateHandler(routingContext: RoutingContext): Future<Void> {
    val params = routingContext.request().params()
    val group = params["group"]
    val name = params["name"]
    val pipeline = routingContext.bodyAsJson

    println(group)
    println(name)
    println(pipeline)

    return toJsonResponse(routingContext, pipeline)
}

fun apiSpecHandler(routingContext: RoutingContext): Future<Void> =
    routingContext.response()
        .putHeader("content-type", "application/yaml")
        .end({}.javaClass.getResource("bob/api.yaml").readText())

fun pipelineArtifactHandler(routingContext: RoutingContext): Future<Void> =
    routingContext.response()
        .sendFile("test.tar.gz")