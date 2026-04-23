package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import jakarta.persistence.EntityManager
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
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.TestFlywayConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.container.PostgresContainer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.wiremock.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.wiremock.ManageUsersMockServer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection.*
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprSystemAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.AsyncDataApiService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["spring.main.allow-bean-definition-overriding=true"])
@ActiveProfiles("test")
@Import(TestFlywayConfig::class)
@AutoConfigureWebTestClient
class ProductCollectionIntegrationTest {

  @Value("\${dpr.lib.user.role}")
  lateinit var authorisedRole: String

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  @Autowired
  lateinit var authenticationHelper: TestAuthenticationHelper

  @Autowired
  lateinit var productCollectionRepository: ProductCollectionRepository

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @MockitoBean
  lateinit var asyncDataApiService: AsyncDataApiService

  companion object {

    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
    }

    val pgContainer = PostgresContainer.instance

    @JvmField
    val hmppsAuthMockServer = HmppsAuthMockServer()

    @JvmField
    val manageUsersMockServer = ManageUsersMockServer()

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
      hmppsAuthMockServer.start()
      manageUsersMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun teardownClass() {
      hmppsAuthMockServer.stop()
      manageUsersMockServer.stop()
    }

    const val TEST_TOKEN = "TestToken"
  }

  @BeforeEach
  fun setup() {
    transactionTemplate.executeWithoutResult {
      entityManager.createNativeQuery("TRUNCATE product_.product_collection CASCADE").executeUpdate()
    }
    val jwt = mock<Jwt>()
    val authentication = mock<DprSystemAuthAwareAuthenticationToken>()
    whenever(jwt.tokenValue).then { TEST_TOKEN }
    whenever(authentication.jwt).then { jwt }
    authenticationHelper.authentication = authentication
    hmppsAuthMockServer.stubGrantToken()
    manageUsersMockServer.stubLookupUsersRoles("request-user", listOf("INCIDENT_REPORTS__RO", "PRISONS_REPORTING_USER"))
    manageUsersMockServer.stubLookupUserCaseload(
      "request-user",
      "ABC",
      """
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
      """.trimIndent(),
    )
    manageUsersMockServer.stubGetUserInfo("request-user", "ABC")
  }

  @Test
  fun `Getting product collections for a user with caseloads but collections have no caseload restrictions returns all collections`() {
    val pc1 = productCollectionRepository.save(
      ProductCollection(name = "coll1", version = "1", ownerName = "bob", products = mutableSetOf(), attributes = mutableSetOf()),
    )
    val pc2 = productCollectionRepository.save(ProductCollection("coll2", "1", "jane", mutableSetOf(), mutableSetOf()))
    val pc3 = productCollectionRepository.save(ProductCollection("coll3", "1", "marley", mutableSetOf(), mutableSetOf()))

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
      .expectBody<Collection<ProductCollectionSummary>>()
      .returnResult()
      .responseBody

    assertThat(productCollections?.filter { it.name == pc1.name }).hasSize(1)
    assertThat(productCollections?.filter { it.name == pc2.name }).hasSize(1)
    assertThat(productCollections?.filter { it.name == pc3.name }).hasSize(1)
    assertThat(productCollections).size().isEqualTo(3)
  }

  @Test
  fun `Getting product collections for a user with all caseloads with all caseload restrictions returns all collections`() {
    productCollectionRepository.save(
      ProductCollection(
        "coll1",
        "1",
        "bob",
        mutableSetOf(ProductCollectionProduct("123")),
        mutableSetOf(
          ProductCollectionAttribute("caseloads", "ABC"),
          ProductCollectionAttribute("caseloads", "DEF"),
        ),
      ),
    )
    productCollectionRepository.save(
      ProductCollection(
        "coll2",
        "1",
        "jane",
        mutableSetOf(ProductCollectionProduct("456")),
        mutableSetOf(
          ProductCollectionAttribute("caseloads", "ABC"),
        ),
      ),
    )
    productCollectionRepository.save(
      ProductCollection(
        "coll3",
        "1",
        "marley",
        mutableSetOf(),
        mutableSetOf(
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
      .expectBody<Collection<ProductCollectionSummary>>()
      .returnResult()
      .responseBody

    assertThat(productCollections).size().isEqualTo(3)
    assertThat(productCollections?.filter { it.name == "coll1" }).hasSize(1)
    assertThat(productCollections?.filter { it.name == "coll2" }).hasSize(1)
    assertThat(productCollections?.filter { it.name == "coll3" }).hasSize(1)
  }

  @Test
  fun `Getting product collections for a user with caseloads but collections have mixed restrictions returns 2 collections`() {
    manageUsersMockServer.stubLookupUserCaseload(
      "request-user",
      "ABC",
      """
        "caseloads": [
          {
            "id": "ABC",
            "name": "ABCPRISON (ABC)"
          }
        ]
      """.trimIndent(),
    )
    productCollectionRepository.save(ProductCollection("coll1", "1", "bob", mutableSetOf(ProductCollectionProduct("123")), mutableSetOf()))
    productCollectionRepository.save(
      ProductCollection(
        "coll2",
        "1",
        "jane",
        mutableSetOf(ProductCollectionProduct("456")),
        mutableSetOf(
          ProductCollectionAttribute("caseloads", "ABC"),
        ),
      ),
    )
    productCollectionRepository.save(
      ProductCollection(
        "coll3",
        "1",
        "marley",
        mutableSetOf(ProductCollectionProduct("456")),
        mutableSetOf(
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
      .expectBody<Collection<ProductCollectionSummary>>()
      .returnResult()
      .responseBody

    assertThat(productCollections?.filter { it.name == "coll1" }).hasSize(1)
    assertThat(productCollections?.filter { it.name == "coll2" }).hasSize(1)
    assertThat(productCollections?.filter { it.name == "coll3" }).hasSize(0)
    assertThat(productCollections).size().isEqualTo(2)
  }

  @Test
  fun `Getting product collections for a user with caseloads shows you only need to match one attribute value`() {
    productCollectionRepository.save(
      ProductCollection(
        "coll2",
        "1",
        "jane",
        mutableSetOf(ProductCollectionProduct("456")),
        mutableSetOf(
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
      .expectBody<Collection<ProductCollectionSummary>>()
      .returnResult()
      .responseBody

    assertThat(productCollections?.filter { it.name == "coll2" }).hasSize(1)
    assertThat(productCollections).size().isEqualTo(1)
  }

  @Test
  fun `Getting single product collection by id succeeds`() {
    val coll = productCollectionRepository.save(
      ProductCollection(
        "coll2",
        "1",
        "jane",
        mutableSetOf(ProductCollectionProduct("456")),
        mutableSetOf(
          ProductCollectionAttribute("caseloads", "DEF"),
        ),
      ),
    )

    println("\n**coll: $coll.id**\n")

    val productCollections = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/productCollections/${coll.id}")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole), jwtAuthorisationHelper = jwtAuthorisationHelper))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<ProductCollectionDTO>()
      .returnResult()
      .responseBody

    assertThat(productCollections).isNotNull()
    assertThat(productCollections!!.products).hasSize(1)
    assertThat(productCollections.name).isEqualTo("coll2")
    assertThat(productCollections.id).isEqualTo(coll.id)
  }

  @Test
  fun `Getting single product collection by id fails`() {
    productCollectionRepository.save(
      ProductCollection(
        "coll2",
        "1",
        "jane",
        mutableSetOf(ProductCollectionProduct("456")),
        mutableSetOf(
          ProductCollectionAttribute("caseloads", "ABC"),
          ProductCollectionAttribute("caseloads", "DEF"),
        ),
      ),
    )

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/productCollections/abc123")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole), jwtAuthorisationHelper = jwtAuthorisationHelper))
      .exchange()
      .expectStatus()
      .isBadRequest
  }
}
