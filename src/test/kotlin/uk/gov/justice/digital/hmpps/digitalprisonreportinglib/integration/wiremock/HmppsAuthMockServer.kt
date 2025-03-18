package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.stubbing.StubMapping

const val HMPPS_AUTH_WIREMOCK_PORT = 8090

class HmppsAuthMockServer : MockServer(HMPPS_AUTH_WIREMOCK_PORT, "/auth") {
  fun stubGrantToken(): StubMapping = stubFor(
    post(urlEqualTo("$urlPrefix/oauth/token"))
      .willReturn(
        aResponse()
          .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
          .withBody(
            // language=json
            """
              {
                "token_type": "bearer",
                "access_token": "ABCDE"
              }
              """,
          ),
      ),
  )
}
