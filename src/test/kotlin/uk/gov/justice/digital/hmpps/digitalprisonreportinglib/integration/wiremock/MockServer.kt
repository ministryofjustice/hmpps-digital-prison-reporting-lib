package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.stubbing.StubMapping

abstract class MockServer(
  port: Int,
  val urlPrefix: String = "",
) : WireMockServer(port) {
  protected val mapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

  fun stubHealthPing(status: Int): StubMapping {
    val statusWord = if (status == 200) "UP" else "DOWN"
    return stubFor(
      get("$urlPrefix/health/ping").willReturn(
        aResponse()
          .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
          .withBody(
            // language=json
            """
            {
              "status": "$statusWord"
            }
            """,
          )
          .withStatus(status),
      ),
    )
  }
}
