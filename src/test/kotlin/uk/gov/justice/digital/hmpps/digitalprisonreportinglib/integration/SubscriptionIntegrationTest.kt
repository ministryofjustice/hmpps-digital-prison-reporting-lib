package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration.IntegrationTestBase.Companion.TEST_TOKEN
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprSystemAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription.UserReportSubscription
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription.UserSubscription
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription.UserSubscriptionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription.UserSubscriptionStatus
import java.time.LocalDateTime

class SubscriptionIntegrationTest : IntegrationSystemTestBase() {

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
  val payload = """
      {
      "reportId":"report1234",
      "reportVariantId":"reportVariant1234"
      }
  """.trimIndent()

  @BeforeEach
  override fun setup() {
    val jwt = mock<Jwt>()
    val authentication = mock<DprSystemAuthAwareAuthenticationToken>()
    whenever(jwt.tokenValue).then { TEST_TOKEN }
    whenever(authentication.jwt).then { jwt }
    authenticationHelper.authentication = authentication
    hmppsAuthMockServer.stubGrantToken()

    manageUsersMockServer.stubLookupUsersRoles(userId, listOf("PRISONS_REPORTING_USER"))
    manageUsersMockServer.stubGetUserInfo(userId)
    manageUsersMockServer.stubLookupUserCaseload()

    transactionTemplate.executeWithoutResult {
      entityManager.createNativeQuery("TRUNCATE subscription_.user_subscription CASCADE").executeUpdate()
    }
  }

  @Test
  fun `Subscribe to a report for user`() {
    val result = webTestClient.post()
      .uri("/user/subscribe")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(payload)
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus().isOk
      .expectBody<UserSubscription>()
      .returnResult()
    assertThat(result.responseBody).isNotNull()
    assertThat(result.responseBody!!.userId).isEqualTo("request-user")
    assertThat(result.responseBody!!.status).isEqualTo("SUBSCRIBED")
  }

  @Test
  fun `Unsubscribe to a report for user`() {
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

    val result = webTestClient.post()
      .uri("/user/unsubscribe")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(payload)
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus().isOk
      .expectBody<UserSubscription>()
      .returnResult()
    assertThat(result.responseBody).isNotNull()
    assertThat(result.responseBody!!.userId).isEqualTo("request-user")
    assertThat(result.responseBody!!.status).isEqualTo("UNSUBSCRIBED")
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
    assertThat(userSubscriptions!!.first().userId).isEqualTo("request-user")
    assertThat(userSubscriptions!!.first().reportId).isEqualTo("report1234")
    assertThat(userSubscriptions!!.first().reportVariantId).isEqualTo("reportVariant1234")
  }
}
