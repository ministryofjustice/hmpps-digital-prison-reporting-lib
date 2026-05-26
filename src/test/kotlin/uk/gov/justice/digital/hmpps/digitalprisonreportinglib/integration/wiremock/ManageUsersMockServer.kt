package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

const val MANAGE_USERS_WIREMOCK_PORT = 8082

class ManageUsersMockServer : MockServer(MANAGE_USERS_WIREMOCK_PORT) {
  fun stubLookupUserCaseload(
    username: String = "request-user",
    activeCaseloadId: String? = "WWI",
    caseloads: String = """
      [
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
    """.trimIndent(),
  ) {
    val payloadArr = mutableListOf(
      """
          {
            "username": "$username",
            "authSource": "NOMIS",
            "active": true,
            "accountType": "GENERAL"
      """.trimIndent(),
    )

    if (activeCaseloadId != null) {
      payloadArr.add(
        """
        "activeCaseload": {
          "id": "$activeCaseloadId",
          "name": "WANDSWORTH (HMP)"
        }
        """.trimIndent(),
      )
    }

    payloadArr.add("\"caseloads\":${(caseloads).trimIndent()}")

    val payload = payloadArr.joinToString(",") + "}"
    stubFor(
      get("$urlPrefix/prisonusers/$username/caseloads")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(payload).withStatus(200),
        ),
    )
  }

  fun stubLookupUserCaseload404(username: String) {
    stubFor(
      get("$urlPrefix/prisonusers/$username/caseloads")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404),
        ),
    )
  }

  fun stub500ResponseInitial(url: String) {
    stubFor(
      get("$urlPrefix/$url")
        .inScenario("retry")
        .whenScenarioStateIs(STARTED)
        .willReturn(aResponse().withStatus(500))
        .willSetStateTo("second"),
    )
  }

  fun stub500ResponseSecondRequest(url: String) {
    stubFor(
      get("$urlPrefix/$url")
        .inScenario("retry")
        .whenScenarioStateIs("second")
        .willReturn(aResponse().withStatus(500))
        .willSetStateTo("success"),
    )
  }

  fun stubSuccessInThirdRequest(url: String) {
    stubFor(
      get("$urlPrefix/$url")
        .inScenario("retry")
        .whenScenarioStateIs("success")
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(createPayload()),
        ),
    )
  }

  fun stubGetUserInfoSuccessAfterRetry(username: String = "request-user", authSource: AuthSource = AuthSource.NOMIS) {
    stubFor(
      get("$urlPrefix/users/$username")
        .inScenario("retry")
        .whenScenarioStateIs("success")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """{
                "username":"$username",
                "active":true,
                "name":"$username",
                "authSource":"${authSource.name}",
                "staffId":488253,
                "activeCaseLoadId":"KMI",
                "userId":"488253",
                "uuid":"bc04893a-1d41-4c66-9dc0-2f77c2b97c65"
              }
              """.trimMargin(),
            ).withStatus(200),
        ),
    )
  }

  fun verifyTimesCalled(url: String, times: Int) {
    verify(
      times,
      getRequestedFor(urlEqualTo(url)),
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

  fun stubGetUserInfo(username: String = "request-user", authSource: AuthSource = AuthSource.NOMIS) {
    stubFor(
      get("$urlPrefix/users/$username")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """{
                "username":"$username",
                "active":true,
                "name":"$username",
                "authSource":"${authSource.name}",
                "staffId":488253,
                "activeCaseLoadId":"KMI",
                "userId":"488253",
                "uuid":"bc04893a-1d41-4c66-9dc0-2f77c2b97c65"
              }
              """.trimMargin(),
            ).withStatus(200),
        ),
    )
  }

  private fun Any.toJson(): String = ObjectMapper().writeValueAsString(this)
  data class RolesResponse(
    val roleCode: String,
  )

  private fun createPayload(): String {
    val username = "request-user"
    val activeCaseloadId = "WWI"
    val caseloads: String = """
      [
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
    """.trimIndent()
    val payloadArr = mutableListOf(
      """
          {
            "username": "$username",
            "authSource": "NOMIS",
            "active": true,
            "accountType": "GENERAL"
      """.trimIndent(),
    )

    if (activeCaseloadId != null) {
      payloadArr.add(
        """
        "activeCaseload": {
          "id": "$activeCaseloadId",
          "name": "WANDSWORTH (HMP)"
        }
        """.trimIndent(),
      )
    }

    payloadArr.add("\"caseloads\":${(caseloads).trimIndent()}")

    return payloadArr.joinToString(",") + "}"
  }
}
