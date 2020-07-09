import io.javalin.http.Context
import java.util.*

data class RequestContext(
    val userId: UUID? = null,
    val organization: Organization? = null,
    val homeLocation: Location? = null,
    private val locations: List<Location> = emptyList(),
    val httpRequestContext: Context
) {

    val locationIds: IntArray
        get() {
            val result = IntArray(locations.size)
            for (i in result.indices) {
                result[i] = locations[i].id
            }
            return result
        }

}