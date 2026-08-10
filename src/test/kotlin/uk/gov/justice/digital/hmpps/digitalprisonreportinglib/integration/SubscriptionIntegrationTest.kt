package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.TestFlywayConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.container.PostgresContainer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription.UserReportSubscription
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription.UserSubscription
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription.UserSubscriptionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription.UserSubscriptionStatus
import java.time.LocalDateTime

class SubscriptionIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var userSubscriptionRepository: UserSubscriptionRepository

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  companion object {

    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.aws.accountId") { "1" }
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
    }
  }

  val userId = "request-user"

  @BeforeEach
  override fun setup() {
    manageUsersMockServer.stubLookupUsersRoles(userId, listOf("PRISONS_REPORTING_USER"))
    manageUsersMockServer.stubLookupUserCaseload()


    transactionTemplate.executeWithoutResult {
      entityManager.createNativeQuery("TRUNCATE subscription_.user_subscription CASCADE").executeUpdate()
    }

    //SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    //SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);

    //manageUsersMockServer.stubLookupUsersRoles(userId, listOf("PRISONS_REPORTING_USER"))
    //manageUsersMockServer.stubLookupUserCaseload()
  }

  @Test
  fun `Subscribe to a report for user`() {

    val payload = """
      {
      "reportId":"report1234",
      "reportVariantId":"reportVariant1234"
      }
    """.trimIndent()


    val userSubscriptions = webTestClient.post()
      .uri("/user/subscribe")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(payload)
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus().isOk
      //.expectBody<Collection<UserReportSubscription>>()
      //.returnResult()
      //.responseBody

    //assertThat(userSubscriptions).size().isEqualTo(1)
  }


  @Test
  fun `Getting User Subscriptions for given user`() {
    val us = userSubscriptionRepository.create(
      UserSubscription(
        id = "1234",
        userId = userId,
        reportId = "report1234",
        reportVariantId = "reportVariant1234",
        status = UserSubscriptionStatus.SUBSCRIBED.name,
        createdTime = LocalDateTime.now(),
      ),
    )

    val userSubscriptions = webTestClient.get()
      .uri("/user/subscriptions")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus().isOk
      .expectBody<Collection<UserReportSubscription>>()
      .returnResult()
      .responseBody

    assertThat(userSubscriptions).size().isEqualTo(1)
  }

  /*
  @Test
  fun `Getting product collections for a user with all caseloads with all caseload restrictions returns all collections`() {
    userSubscriptionRepository.save(
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
    userSubscriptionRepository.save(
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
    userSubscriptionRepository.save(
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
        [
          {
            "id": "ABC",
            "name": "ABCPRISON (ABC)"
          }
        ]
      """.trimIndent(),
    )
    userSubscriptionRepository.save(ProductCollection("coll1", "1", "bob", mutableSetOf(ProductCollectionProduct("123")), mutableSetOf()))
    userSubscriptionRepository.save(
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
    userSubscriptionRepository.save(
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
    userSubscriptionRepository.save(
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
    val coll = userSubscriptionRepository.save(
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
    userSubscriptionRepository.save(
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

   */
}
