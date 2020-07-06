import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import graphql.ExecutionInput
import graphql.GraphQL
import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.http.HandlerType
import schemabuilder.processor.pipelines.parsing.dataloaders.DataLoaderRepository
import java.util.HashMap


class DashflightGraphQLPlugin(
    private val props: JavalinProperties,
    private val graphql: GraphQL,
    private val contextProvider: GraphQLContextProvider? = null
): Plugin {

    override fun apply(javalin: Javalin) {
        // Log exceptions to console
        javalin.exception(Exception::class.java) { exception, _ ->
            exception.printStackTrace()
        }

        // Block trace requests due to old vulnerability with HttpOnly cookie setting
        javalin.addHandler(HandlerType.TRACE, "*") {
            it.res.status = 405
        }

        // Allow options for pre-flight requests
        javalin.addHandler(HandlerType.OPTIONS, "*") {}


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

        // Used for health checks by docker
        javalin.get("${props.endpoint}/ping") { it.result("pong") }

        //============================GraphQL Configuration=================================
        val mapper = ObjectMapper().registerModule(KotlinModule())
        // configuration.graphQL.transform { builder: GraphQL.Builder -> builder.instrumentation(configuration.instrumentation) }
        javalin.post(props.endpoint) {
            val ctx: Any?
            val token = it.req.getHeader("Authorization")?.replace("Bearer ", "")
            val tokenFgp = it.req.cookies?.let { list ->
                list.first { cookie ->
                    cookie.name === "Secure-Fgp"
                }
            }

            ctx = contextProvider?.createContext(token, tokenFgp?.value)

            val body = it.body()

            val data: Map<String, Any?> = mapper.readValue(
                body,
                object : TypeReference<HashMap<String, Any?>>() {}
            )

            try {
                val input = ExecutionInput.newExecutionInput()
                    .query(data["query"] as String?)
                    .variables(data["variables"] as Map<String?, Any?>?)
                    .dataLoaderRegistry(DataLoaderRepository.dataLoaderRegistry)
                    .context(ctx)
                    .build()
                it.result(mapper.writeValueAsString(graphql.execute(input)?.toSpecification()))
            } catch (e: ClassCastException) {
                it.res.sendError(400, "The variables supplied were malformed")
            }
            // it.res.sendError(400, "Whoops! Something went wrong")
        }
    }
}