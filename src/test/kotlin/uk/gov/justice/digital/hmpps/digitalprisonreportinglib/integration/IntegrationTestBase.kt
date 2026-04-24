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
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.TestFlywayConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.container.PostgresContainer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ExternalMovementRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.PrisonerRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.alert.AlertCategoryRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings.EstablishmentsToWingsRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.wiremock.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.wiremock.ManageUsersMockServer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprSystemAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.UserPermissionProvider
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.AsyncDataApiService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import uk.gov.justice.hmpps.kotlin.auth.AuthSource
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = ["spring.main.allow-bean-definition-overriding=true"])
@ActiveProfiles("test")
@Import(TestFlywayConfig::class)
@AutoConfigureWebTestClient
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

  @Autowired
  lateinit var configuredApiRepository: ConfiguredApiRepository

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var userPermissionProvider: UserPermissionProvider

  @MockitoBean
  lateinit var dynamoDbClient: DynamoDbClient

  @MockitoBean
  lateinit var establishmentsToWingsRepository: EstablishmentsToWingsRepository

  @MockitoBean
  lateinit var alertCategoryRepository: AlertCategoryRepository

  @MockitoBean
  lateinit var asyncDataApiService: AsyncDataApiService

  companion object {

    @JvmField
    val hmppsAuthMockServer = HmppsAuthMockServer()

    @JvmField
    val manageUsersMockServer = ManageUsersMockServer()

    lateinit var wireMockServer: WireMockServer
    val pgContainer = PostgresContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun setupClass(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
      }
    }

    @BeforeAll
    @JvmStatic
    fun setupClass() {
      wireMockServer = WireMockServer(
        WireMockConfiguration.wireMockConfig().port(9999),
      )
      wireMockServer.start()
      hmppsAuthMockServer.start()
      manageUsersMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun teardownClass() {
      wireMockServer.stop()
      hmppsAuthMockServer.stop()
      manageUsersMockServer.stop()
    }

    const val TEST_TOKEN = "TestToken"
  }

  @BeforeEach
  fun setup() {
    wireMockServer.resetAll()
    stubDefinitionsResponse()
    hmppsAuthMockServer.stubGrantToken()
    manageUsersMockServer.stubLookupUsersRoles("request-user", listOf("INCIDENT_REPORTS__RO", "PRISONS_REPORTING_USER"))
    manageUsersMockServer.stubLookupUserCaseload("request-user", "LWSTMC")
    manageUsersMockServer.stubGetUserInfo()
    ConfiguredApiRepositoryTest.AllMovements.allExternalMovements.forEach {
      externalMovementRepository.save(it)
    }
    ConfiguredApiRepositoryTest.AllPrisoners.allPrisoners.forEach {
      prisonerRepository.save(it)
    }
    val jwt = mock<Jwt>()
    val authentication = mock<DprSystemAuthAwareAuthenticationToken>()
    whenever(jwt.tokenValue).then { TEST_TOKEN }
    whenever(authentication.jwt).then { jwt }
    whenever(authentication.authSource).then { AuthSource.NONE }
    whenever(authentication.name).then { "TESTUSER1" }
    whenever(authentication.getUsername()).then { "TESTUSER1" }
    whenever(authentication.userName).then { "TESTUSER1" }
    whenever(authentication.getCaseLoads()).then {
      listOf(
        Caseload("ABC", "ABCPRISON (ABC)"),
        Caseload("DEF", "DEFPRISON (DEF)"),
        Caseload("GHI", "GHIPRISON (GHI)"),
        Caseload("LWSTMC", "Lowestoft (North East Suffolk) Magistrat"),
        Caseload("WWI", "WANDSWORTH (HMP)"),
        Caseload("AKI", "Acklington (HMP)"),
      )
    }
    whenever(authentication.getCaseLoadIds()).then { listOf("ABC", "DEF", "GHI", "LWSTMC", "WWI", "AKI") }
    whenever(authentication.getActiveCaseLoadId()).then { "LWSTMC" }
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

  protected fun createCaseloadJsonResponse(activeCaseloadId: String) =
    """
      {
        "username": "TESTUSER1",
        "authSource": "NONE",
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

fun setAuthorisation(
  user: String = "request-user",
  roles: List<String> = emptyList(),
  scopes: List<String> = emptyList(),
  jwtAuthorisationHelper: JwtAuthorisationHelper,
): (HttpHeaders) -> Unit = jwtAuthorisationHelper.setAuthorisationHeader(
  clientId = "hmpps-digital-prison-reporting-api",
  username = user,
  scope = scopes,
  roles = roles,
)
