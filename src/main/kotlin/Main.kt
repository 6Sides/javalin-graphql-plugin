import graphql.GraphQL
import graphql.schema.GraphQLSchema
import graphql.schema.StaticDataFetcher
import graphql.schema.idl.*
import hydro.engine.FileDataSource
import hydro.engine.Hydro
import hydro.engine.YAMLConfiguration
import io.javalin.Javalin

fun main() {
    Hydro.configure {
        namespace("test") {
            bindNamespace<HydroJavalinProperties>("test")
            YAMLConfiguration(FileDataSource("development.yaml"))
        }
    }

    val props = HydroJavalinProperties().getProperties()
    Javalin.create {
        it.registerPlugin(DashflightPlugin(props))
        it.registerPlugin(GraphQLPlugin("/test", createGraphQL()))
    }.start(props.port)
}

private fun createGraphQL(): GraphQL {
    val schema = "type Query{hello: String}"

    val schemaParser = SchemaParser()
    val typeDefinitionRegistry: TypeDefinitionRegistry = schemaParser.parse(schema)

    val runtimeWiring = RuntimeWiring.newRuntimeWiring()
        .type("Query") { builder: TypeRuntimeWiring.Builder -> builder.dataFetcher("hello", StaticDataFetcher("world")) }
        .build()

    val schemaGenerator = SchemaGenerator()
    val graphQLSchema: GraphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)

    return GraphQL.newGraphQL(graphQLSchema).build()
}