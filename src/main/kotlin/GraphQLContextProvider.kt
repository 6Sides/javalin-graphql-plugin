interface GraphQLContextProvider {
    fun createContext(token: String?, tokenFgp: String?): Any?
}