package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricDataResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.MetricsDataService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MetricsDataIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var metricsDataService: MetricsDataService

  @MockBean
  lateinit var productDefinitionRepository: ProductDefinitionRepository

  @Test
  fun `Calling the metrics data endpoint calls the metricsDataService with the correct arguments and returns the data`() {
    val expectedServiceResult = listOf(
      mapOf(
        "establishment_id" to "WWI",
        "missing_ethnicity_percentage" to 2,
        "present_ethnicity_percentage" to 98,
        "no_of_prisoners" to 196,
        "no_of_prisoners_without" to 4,
        "random_data" to 20,
      ),
    )
    given(
      metricsDataService.validateAndFetchData(
        eq("missing-ethnicity-metrics"),
        eq("missing-ethnicity-metric"),
        any<DprAuthAwareAuthenticationToken>(),
        eq("definitions/prisons/orphanage"),
      ),
    )
      .willReturn(expectedServiceResult)

    val result = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/missing-ethnicity-metrics/metrics/missing-ethnicity-metric")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody(MetricDataResponse::class.java)
      .returnResult()

    assertThat(result.responseBody).isNotNull
    val metric = result.responseBody!!

    assertThat(metric.id).isEqualTo("missing-ethnicity-metric")
    assertThat(metric.data).isEqualTo(expectedServiceResult)
    assertThat(metric.updated).isNotNull
  }
}
