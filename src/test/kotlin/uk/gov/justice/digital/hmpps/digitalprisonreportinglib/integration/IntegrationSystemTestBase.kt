package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.TestFlywayConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.TestWebClientConfiguration
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.container.PostgresContainer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ExternalMovementRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.PrisonerRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings.EstablishmentsToWingsRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.IntegrationTestBase.Companion.TEST_TOKEN
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.wiremock.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.wiremock.ManageUsersMockServer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprSystemAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.AsyncDataApiService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = ["spring.main.allow-bean-definition-overriding=true"])
@ActiveProfiles("system-test")
@Import(TestWebClientConfiguration::class, TestFlywayConfig::class)
abstract class IntegrationSystemTestBase {

  @Value("\${dpr.lib.system.role}")
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
    @Suppress("unused")
    @JvmField
    val hmppsAuthMockServer = HmppsAuthMockServer()

    @JvmField
    val manageUsersMockServer = ManageUsersMockServer()
    val postgresContainer = PostgresContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun setupClass(registry: DynamicPropertyRegistry) {
      postgresContainer?.run {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
        registry.add("spring.datasource.username", postgresContainer::getUsername)
        registry.add("spring.datasource.password", postgresContainer::getPassword)
      }
    }

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      hmppsAuthMockServer.start()
      hmppsAuthMockServer.stubGrantToken()
      manageUsersMockServer.start()

      hmppsAuthMockServer.stubGrantToken()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      manageUsersMockServer.stop()
      hmppsAuthMockServer.stop()
    }
  }

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  @BeforeEach
  fun setup() {
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
    authenticationHelper.authentication = authentication
    whenever(authentication.getRoles()).thenReturn(listOf(authorisedRole))
    whenever(authentication.getCaseLoads()).thenReturn(listOf(Caseload("WWI", "WANDSWORTH (HMP)")))
    whenever(authentication.getActiveCaseLoadId()).thenReturn("WWI")
    whenever(authentication.getCaseLoadIds()).thenReturn(listOf("WWI"))
    whenever(authentication.getUsername()).thenReturn("request-user")
  }

  protected fun setAuthorisation(
    user: String? = "request-user",
    roles: List<String> = emptyList(),
    scopes: List<String> = emptyList(),
  ): (HttpHeaders) -> Unit = jwtAuthorisationHelper.setAuthorisationHeader(
    clientId = "hmpps-digital-prison-reporting-api",
    username = user,
    scope = scopes,
    roles = roles,
  )
}
