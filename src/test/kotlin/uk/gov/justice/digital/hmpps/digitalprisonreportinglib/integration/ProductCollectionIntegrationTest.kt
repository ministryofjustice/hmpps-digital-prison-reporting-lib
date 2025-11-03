package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.TestFlywayConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.container.PostgresContainer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductCollectionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection.ProductCollection
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection.ProductCollectionAttribute
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection.ProductCollectionDTO
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection.ProductCollectionProduct
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprUserAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.AsyncDataApiService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["spring.main.allow-bean-definition-overriding=true"])
@ActiveProfiles("test")
@Import(TestFlywayConfig::class)
class ProductCollectionIntegrationTest {

  @Value("\${dpr.lib.user.role}")
  lateinit var authorisedRole: String

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  @Autowired
  lateinit var authenticationHelper: TestAuthenticationHelper

  @Autowired
  lateinit var productCollectionRepository: ProductCollectionRepository

  @MockitoBean
  lateinit var asyncDataApiService: AsyncDataApiService

  companion object {

    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
    }

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
    productCollectionRepository.deleteAll()
    productCollectionRepository.flush()
    val jwt = mock<Jwt>()
    val authentication = mock<DprUserAuthAwareAuthenticationToken>()
    whenever(jwt.tokenValue).then { TEST_TOKEN }
    whenever(authentication.jwt).then { jwt }
    authenticationHelper.authentication = authentication
  }

  @Test
  fun `Getting product collections for a user with caseloads but collections have no caseload restrictions returns all collections`() {
    stubMeCaseloadsResponse(
      """
      {
        "username": "TESTUSER1",
        "active": true,
        "accountType": "GENERAL",
        "activeCaseload": {
          "id": "ABC",
          "name": "ABCPRISON (ABC)"
        },
        "caseloads": [
          {
            "id": "ABC",
            "name": "ABCPRISON (ABC)"
          },
          {
            "id": "DEF",
            "name": "DEFPRISON (DEF)"
          },
          {
            "id": "GHI",
            "name": "GHIPRISON (GHI)"
          }
        ]
      }
      """.trimIndent(),
      wireMockServer,
    )
    val pc1 = productCollectionRepository.saveAndFlush(ProductCollection("coll1", "1", "bob", emptySet(), emptySet()))
    val pc2 = productCollectionRepository.saveAndFlush(ProductCollection("coll2", "1", "jane", emptySet(), emptySet()))
    val pc3 = productCollectionRepository.saveAndFlush(ProductCollection("coll3", "1", "marley", emptySet(), emptySet()))

    val productCollections = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/productCollections")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole), jwtAuthorisationHelper = jwtAuthorisationHelper))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<Collection<ProductCollectionDTO>>()
      .returnResult()
      .responseBody

    assertThat(productCollections?.filter { it.name == pc1.name }).hasSize(1)
    assertThat(productCollections?.filter { it.name == pc2.name }).hasSize(1)
    assertThat(productCollections?.filter { it.name == pc3.name }).hasSize(1)
    assertThat(productCollections).size().isEqualTo(3)
  }

  @Test
  fun `Getting product collections for a user with all caseloads with all caseload restrictions returns all collections`() {
    stubMeCaseloadsResponse(
      """
      {
        "username": "TESTUSER1",
        "active": true,
        "accountType": "GENERAL",
        "activeCaseload": {
          "id": "ABC",
          "name": "ABCPRISON (ABC)"
        },
        "caseloads": [
          {
            "id": "ABC",
            "name": "ABCPRISON (ABC)"
          },
          {
            "id": "DEF",
            "name": "DEFPRISON (DEF)"
          },
          {
            "id": "GHI",
            "name": "GHIPRISON (GHI)"
          }
        ]
      }
      """.trimIndent(),
      wireMockServer,
    )
    val pc1 = productCollectionRepository.save(
      ProductCollection(
        "coll1",
        "1",
        "bob",
        setOf(ProductCollectionProduct("123")),
        setOf(
          ProductCollectionAttribute("caseloads", "ABC"),
          ProductCollectionAttribute("caseloads", "DEF"),
        ),
      ),
    )
    val pc2 = productCollectionRepository.save(
      ProductCollection(
        "coll2",
        "1",
        "jane",
        setOf(ProductCollectionProduct("456")),
        setOf(
          ProductCollectionAttribute("caseloads", "ABC"),
        ),
      ),
    )
    val pc3 = productCollectionRepository.save(
      ProductCollection(
        "coll3",
        "1",
        "marley",
        emptySet(),
        setOf(
          ProductCollectionAttribute("caseloads", "ABC"),
          ProductCollectionAttribute("caseloads", "GHI"),
        ),
      ),
    )

    val productCollections = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/productCollections")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole), jwtAuthorisationHelper = jwtAuthorisationHelper))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<Collection<ProductCollectionDTO>>()
      .returnResult()
      .responseBody

    assertThat(productCollections).size().isEqualTo(3)
    assertThat(productCollections?.filter { it.name == "coll1" }).hasSize(1)
    assertThat(productCollections?.filter { it.name == "coll2" }).hasSize(1)
    assertThat(productCollections?.filter { it.name == "coll3" }).hasSize(1)
  }

  @Test
  fun `Getting product collections for a user with caseloads but collections have mixed restrictions returns 2 collections`() {
    stubMeCaseloadsResponse(
      """
      {
        "username": "TESTUSER1",
        "active": true,
        "accountType": "GENERAL",
        "activeCaseload": {
          "id": "ABC",
          "name": "ABCPRISON (ABC)"
        },
        "caseloads": [
          {
            "id": "ABC",
            "name": "ABCPRISON (ABC)"
          }
        ]
      }
      """.trimIndent(),
      wireMockServer,
    )
    val pc1 = productCollectionRepository.save(ProductCollection("coll1", "1", "bob", setOf(ProductCollectionProduct("123")), emptySet()))
    val pc2 = productCollectionRepository.save(
      ProductCollection(
        "coll2",
        "1",
        "jane",
        setOf(ProductCollectionProduct("456")),
        setOf(
          ProductCollectionAttribute("caseloads", "ABC"),
        ),
      ),
    )
    val pc3 = productCollectionRepository.save(
      ProductCollection(
        "coll3",
        "1",
        "marley",
        setOf(ProductCollectionProduct("456")),
        setOf(
          ProductCollectionAttribute("caseloads", "GHI"),
        ),
      ),
    )

    val productCollections = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/productCollections")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole), jwtAuthorisationHelper = jwtAuthorisationHelper))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<Collection<ProductCollectionDTO>>()
      .returnResult()
      .responseBody

    assertThat(productCollections?.filter { it.name == "coll1" }).hasSize(1)
    assertThat(productCollections?.filter { it.name == "coll2" }).hasSize(1)
    assertThat(productCollections?.filter { it.name == "coll3" }).hasSize(0)
    assertThat(productCollections).size().isEqualTo(2)
  }

  @Test
  fun `Getting product collections for a user with caseloads shows you only need to match one attribute value`() {
    stubMeCaseloadsResponse(
      """
      {
        "username": "TESTUSER1",
        "active": true,
        "accountType": "GENERAL",
        "activeCaseload": {
          "id": "ABC",
          "name": "ABCPRISON (ABC)"
        },
        "caseloads": [
          {
            "id": "ABC",
            "name": "ABCPRISON (ABC)"
          }
        ]
      }
      """.trimIndent(),
      wireMockServer,
    )
    productCollectionRepository.save(
      ProductCollection(
        "coll2",
        "1",
        "jane",
        setOf(ProductCollectionProduct("456")),
        setOf(
          ProductCollectionAttribute("caseloads", "ABC"),
          ProductCollectionAttribute("caseloads", "DEF"),
        ),
      ),
    )

    val productCollections = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/productCollections")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole), jwtAuthorisationHelper = jwtAuthorisationHelper))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<Collection<ProductCollectionDTO>>()
      .returnResult()
      .responseBody

    assertThat(productCollections?.filter { it.name == "coll2" }).hasSize(1)
    assertThat(productCollections).size().isEqualTo(1)
  }
}
