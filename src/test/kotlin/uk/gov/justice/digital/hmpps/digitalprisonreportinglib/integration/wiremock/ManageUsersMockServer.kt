package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

const val MANAGE_USERS_WIREMOCK_PORT = 8082

class ManageUsersMockServer : MockServer(MANAGE_USERS_WIREMOCK_PORT) {
  fun stubLookupUserCaseload(
    username: String = "request-user",
    activeCaseloadId: String = "WWI",
  ) {
    val payload = """
          {
            "username": "TESTUSER1",
            "active": true,
            "accountType": "GENERAL",
            "activeCaseload": {
              "id": "$activeCaseloadId",
              "name": "WANDSWORTH (HMP)"
            },
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
  private fun Any.toJson(): String = ObjectMapper().writeValueAsString(this)
  data class RolesResponse(
    val roleCode: String,
  )
}
