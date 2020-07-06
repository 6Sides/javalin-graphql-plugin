import hydro.engine.Hydro.hydrate

class HydroJavalinProperties {
    private val port: Int by hydrate("port")

    private val endpoint: String by hydrate("endpoint")

    private val allowedHeaders: List<String> by hydrate("allowed-headers")

    private val cookieAttributes: List<String> by hydrate("cookie-attributes")

    private val originHeader: String by hydrate("origin")

    fun getProperties(): JavalinProperties {
        return JavalinProperties(
            port,
            endpoint,
            originHeader,
            allowedHeaders,
            cookieAttributes
        )
    }
}

data class JavalinProperties(
    val port: Int = 7000,
    val endpoint: String = "/graphql",
    val originHeader: String = "http://localhost",
    val allowedHeaders: List<String> = emptyList(),
    val cookieAttributes: List<String> = emptyList()
)