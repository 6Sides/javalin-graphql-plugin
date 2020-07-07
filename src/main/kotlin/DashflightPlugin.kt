import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.http.HandlerType


class DashflightPlugin(
    private val props: JavalinProperties
): Plugin {

    override fun apply(javalin: Javalin) {
        // Log exceptions to console
        javalin.exception(Exception::class.java) { exception, _ ->
            exception.printStackTrace()
        }

        // Configure headers
        javalin.before("*") {
            with(it.res) {
                contentType = "application/json"
                addHeader("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS")
                addHeader("Access-Control-Allow-Origin", props.originHeader)
                addHeader("Access-Control-Allow-Credentials", "true")
                addHeader("Access-Control-Allow-Headers", props.allowedHeaders.joinToString(","))
            }
        }

        // Block trace requests due to old vulnerability with HttpOnly cookie setting
        javalin.addHandler(HandlerType.TRACE, "*") {
            it.res.sendError(405)
        }

        // Allow options for pre-flight requests
        javalin.addHandler(HandlerType.OPTIONS, "*") {}

        // Used for health checks by docker
        javalin.get("${props.endpoint}/ping") { it.result("pong") }
    }
}