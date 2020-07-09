import io.javalin.http.Context

interface GraphQLContextProvider {
    fun createContext(context: Context, token: String?, tokenFgp: String?): Any?
}