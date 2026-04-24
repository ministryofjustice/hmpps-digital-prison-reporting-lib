package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

const val MANAGE_USERS_WIREMOCK_PORT = 8082

class ManageUsersMockServer : MockServer(MANAGE_USERS_WIREMOCK_PORT) {
  fun stubLookupUserCaseload(
    username: String = "request-user",
    activeCaseloadId: String? = "WWI",
    caseloads: String? = null,
  ) {
    val payload = """
          {
            "username": "$username",
            "authSource": "NOMIS",
            "active": true,
            "accountType": "GENERAL",
            ${activeCaseloadId?.let {
      """
              "activeCaseload": {
                "id": "$activeCaseloadId",
                "name": "WANDSWORTH (HMP)"
              },
      """.trimIndent()
    } ?: ""}
            ${caseloads ?: """
              "caseloads": [
                {
                  "id": "WWI",
                  "name": "WANDSWORTH (HMP)"
                },
                {
                  "id": "AKI",
                  "name": "Acklington (HMP)"
                },
                {
                  "id": "LWSTMC",
                  "name": "Lowestoft (North East Suffolk) Magistrat"
                }
              ]
    """.trimIndent()}
          }
    """.trimIndent()
    stubFor(
      get("$urlPrefix/prisonusers/$username/caseloads")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(payload).withStatus(200),
        ),
    )
  }

  fun stubLookupUsersRoles(username: String = "request-user", roles: List<String> = emptyList()) {
    stubFor(
      get("$urlPrefix/users/$username/roles")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(roles.map { RolesResponse(it) }.toJson()).withStatus(200),
        ),
    )
  }

  fun stubGetUserInfo(username: String = "request-user", activeCaseloadId: String? = "LWSTMC", authSource: AuthSource = AuthSource.NOMIS) {
    stubFor(
      get("$urlPrefix/users/$username")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              AuthUser(
                username = username,
                active = true,
                name = username,
                authSource = authSource,
                userId = "123456",
                uuid = "1a1a1a-1a1a1a1-1a1a1a1-1a1a1a1",
                staffId = null,
                activeCaseLoadId = activeCaseloadId,
              ).toJson(),
            ).withStatus(200),
        ),
    )
  }

  private fun Any.toJson(): String = ObjectMapper().writeValueAsString(this)
  data class RolesResponse(
    val roleCode: String,
  )
}
