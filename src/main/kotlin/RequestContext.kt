import io.javalin.http.Context
import java.util.*

data class RequestContext(
    val userId: UUID? = null,
    val organization: OrganizationInfo? = null,
    val homeLocation: LocationInfo? = null,
    private val locations: List<LocationInfo> = emptyList(),
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