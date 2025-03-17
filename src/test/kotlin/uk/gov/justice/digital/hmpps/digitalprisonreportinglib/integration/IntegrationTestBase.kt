package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ExternalMovementRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.PrisonerRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings.EstablishmentsToWingsRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprUserAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.AsyncDataApiService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = ["spring.main.allow-bean-definition-overriding=true"])
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Value("\${dpr.lib.user.role}")
  lateinit var authorisedRole: String

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  @Autowired
  lateinit var externalMovementRepository: ExternalMovementRepository

  @Autowired
  lateinit var prisonerRepository: PrisonerRepository

  @Autowired
  lateinit var authenticationHelper: TestAuthenticationHelper

  @MockitoBean
  lateinit var dynamoDbClient: DynamoDbClient

  @MockitoBean
  lateinit var establishmentsToWingsRepository: EstablishmentsToWingsRepository

  @MockitoBean
  lateinit var asyncDataApiService: AsyncDataApiService

  companion object {

    lateinit var wireMockServer: WireMockServer

    @BeforeAll
    @JvmStatic
    fun setupClass() {
      wireMockServer = WireMockServer(
        WireMockConfiguration.wireMockConfig().port(9999),
      )
      wireMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun teardownClass() {
      wireMockServer.stop()
    }

    const val TEST_TOKEN = "TestToken"
  }

  @BeforeEach
  fun setup() {
    wireMockServer.resetAll()
    stubMeCaseloadsResponse(createCaseloadJsonResponse("LWSTMC"))
    stubDefinitionsResponse()
    ConfiguredApiRepositoryTest.AllMovements.allExternalMovements.forEach {
      externalMovementRepository.save(it)
    }
    ConfiguredApiRepositoryTest.AllPrisoners.allPrisoners.forEach {
      prisonerRepository.save(it)
    }
    val jwt = mock<Jwt>()
    val authentication = mock<DprUserAuthAwareAuthenticationToken>()
    whenever(jwt.tokenValue).then { TEST_TOKEN }
    whenever(authentication.jwt).then { jwt }
    authenticationHelper.authentication = authentication
  }

  protected fun stubDefinitionsResponse() {
    val productDefinitionJson = this::class.java.classLoader.getResource("productDefinition.json")?.readText()
    wireMockServer.stubFor(
      WireMock.get("/definitions/prisons/orphanage")
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer $TEST_TOKEN"))
        .willReturn(
          WireMock.aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody("""[$productDefinitionJson]"""),
        ),
    )
  }

  protected fun stubMeCaseloadsResponse(body: String) {
    wireMockServer.stubFor(
      WireMock.get("/users/me/caseloads").willReturn(
        WireMock.aResponse()
          .withStatus(HttpStatus.OK.value())
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .withBody(body),
      ),
    )
  }

  protected fun createCaseloadJsonResponse(activeCaseloadId: String) =
    """
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

  protected fun setAuthorisation(
    user: String = "request-user",
    roles: List<String> = emptyList(),
    scopes: List<String> = emptyList(),
  ): (HttpHeaders) -> Unit = jwtAuthorisationHelper.setAuthorisationHeader(
    clientId = "hmpps-digital-prison-reporting-api",
    username = user,
    scope = scopes,
    roles = roles,
  )
}
