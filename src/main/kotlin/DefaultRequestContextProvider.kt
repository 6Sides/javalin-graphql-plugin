import com.google.inject.Inject
import io.javalin.http.Context
import net.dashflight.data.jwt.verify.JwtVerificationResponse
import net.dashflight.data.jwt.verify.JwtVerifier
import net.dashflight.data.postgres.PostgresClient
import org.postgresql.util.PGobject
import java.util.*

class DefaultRequestContextProvider @Inject constructor(
        private val postgresClient: PostgresClient,
        private val jwtVerifier: JwtVerifier
) : GraphQLContextProvider {

    override fun createContext(context: Context, token: String?, tokenFgp: String?): RequestContext? {
        if (token == null || tokenFgp == null) {
            return RequestContext(httpRequestContext = context)
        }

        var userId: String? = null

        when (val response = jwtVerifier.verifyToken(token, tokenFgp)) {
            is JwtVerificationResponse.Success -> {
                userId = response.result.getClaim("user_id").asString()
            }
            is JwtVerificationResponse.Error -> println(response.message)
        }

        return userId?.let { id ->
            val locations = getLocations(id)
            val (organization, homeLocation) = getOrgAndHomeLocation(id)

            RequestContext(UUID.fromString(id), organization, homeLocation, locations, context)
        } ?: RequestContext(httpRequestContext = context)
    }

    private fun getLocations(userId: String): List<LocationInfo> {
        postgresClient.connection.use { conn ->
            val SQL = ("select locations.id, locations.name from accounts.user_locations "
                    + "inner join accounts.locations on user_locations.location_id = locations.id "
                    + "where user_id = ?")

            val stmt = conn.prepareStatement(SQL)
            val uid = PGobject()
            uid.type = "uuid"
            uid.value = userId
            stmt.setObject(1, uid)
            val res = stmt.executeQuery()

            return res.use {
                generateSequence {
                    if (res.next()) LocationInfo(res.getInt("id"), res.getString("name")) else null
                }.toList()
            }
        }
    }

    private fun getOrgAndHomeLocation(userId: String): Pair<OrganizationInfo, LocationInfo> {
        postgresClient.connection.use { conn ->
            val SQL = ("select organizations.id as org_id, organizations.name as org_name, locations.id as home_location_id, locations.name as home_location_name from accounts.users "
                    + "inner join accounts.organizations on users.organization_id = organizations.id "
                    + "left join accounts.locations on users.home_location_id = locations.id "
                    + "where users.id = ? limit 1")

            val stmt = conn.prepareStatement(SQL)
            val uid = PGobject()
            uid.type = "uuid"
            uid.value = userId
            stmt.setObject(1, uid)
            val res = stmt.executeQuery()

            res.next()

            return Pair(
                OrganizationInfo(
                    res.getInt("org_id"),
                    res.getString("org_name")
                ),
                LocationInfo(
                    res.getInt("home_location_id"),
                    res.getString("home_location_name")
                )
            )
        }
    }
}