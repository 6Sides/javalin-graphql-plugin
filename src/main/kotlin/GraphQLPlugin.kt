import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import graphql.ExecutionInput
import graphql.GraphQL
import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import schemabuilder.processor.pipelines.parsing.dataloaders.DataLoaderRepository
import java.util.HashMap

class GraphQLPlugin(
    private val endpoint: String,
    private val graphql: GraphQL,
    private val contextProvider: GraphQLContextProvider? = null,
    private val enableLogging: Boolean = false
): Plugin {

    private val mapper = ObjectMapper().registerModule(KotlinModule())

    override fun apply(javalin: Javalin) {
        // configuration.graphQL.transform { builder: GraphQL.Builder -> builder.instrumentation(configuration.instrumentation) }
        javalin.post(endpoint) {
            val token = it.req.getHeader("Authorization")?.replace("Bearer ", "")

            // val tokenFgp = it.req.getHeader("Secure-Fgp")
            val tokenFgp = it.req.cookies?.let { list ->
                list.firstOrNull { cookie ->
                    cookie.name == "Secure-Fgp"
                }
            }

            val ctx = contextProvider?.createContext(it, token, tokenFgp)

            val body = it.body()

            if (enableLogging) {
                println(body)
            }

            val data: Map<String, Any?> = mapper.readValue(
                body,
                object : TypeReference<HashMap<String, Any?>>() {}
            )

            try {
                val input = ExecutionInput.newExecutionInput()
                    .query(data["query"] as String?)
                    .variables(data["variables"] as Map<String?, Any?>? ?: emptyMap())
                    .dataLoaderRegistry(DataLoaderRepository.dataLoaderRegistry)
                    .context(ctx)
                    .build()
                it.result(mapper.writeValueAsString(graphql.execute(input)?.toSpecification()))
            } catch (e: ClassCastException) {
                it.res.sendError(400, "The variables supplied were malformed")
            }
        }
    }
}