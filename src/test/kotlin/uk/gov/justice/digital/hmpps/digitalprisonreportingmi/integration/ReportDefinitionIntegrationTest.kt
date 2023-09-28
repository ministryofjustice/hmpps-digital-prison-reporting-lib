package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ReportDefinition

class ReportDefinitionIntegrationTest : IntegrationTestBase() {

  @Test
  fun `Stubbed definition is returned as expected`() {
    val result = webTestClient.get()
      .uri("/definitions")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList<ReportDefinition>()
      .returnResult()

    assertThat(result.responseBody).isNotNull
    assertThat(result.responseBody).hasSize(1)
    assertThat(result.responseBody).first().isNotNull

    val definition = result.responseBody!!.first()

    assertThat(definition.name).isEqualTo("External Movements")
    assertThat(definition.description).isEqualTo("Reports about prisoner external movements")
    assertThat(definition.variants).hasSize(2)
    assertThat(definition.variants[0]).isNotNull
    assertThat(definition.variants[1]).isNotNull

    val lastMonthVariant = definition.variants[0]

    assertThat(lastMonthVariant.id).isEqualTo("last-month")
    assertThat(lastMonthVariant.resourceName).isEqualTo("reports/external-movements/last-month")
    assertThat(lastMonthVariant.name).isEqualTo("Last month")
    assertThat(lastMonthVariant.description).isEqualTo("All movements in the past month")
    assertThat(lastMonthVariant.specification).isNotNull
    assertThat(lastMonthVariant.specification?.fields).hasSize(8)

    val lastWeekVariant = definition.variants[1]
    assertThat(lastWeekVariant.id).isEqualTo("last-week")
    assertThat(lastWeekVariant.resourceName).isEqualTo("reports/external-movements/last-week")
    assertThat(lastWeekVariant.description).isEqualTo("All movements in the past week")
    assertThat(lastWeekVariant.name).isEqualTo("Last week")
    assertThat(lastWeekVariant.specification).isNotNull
    assertThat(lastWeekVariant.specification?.fields).hasSize(8)
  }

  @Test
  fun `Definitions are returned when they match the filter`() {
    val result = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/definitions")
          .queryParam("renderMethod", "HTML")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList<ReportDefinition>()
      .returnResult()

    assertThat(result.responseBody).isNotNull
    assertThat(result.responseBody).hasSize(1)
    assertThat(result.responseBody).first().isNotNull

    val definition = result.responseBody!!.first()

    assertThat(definition.variants).hasSize(2)
  }

  @Test
  fun `Definitions are not returned when they do not match the filter`() {
    val result = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/definitions")
          .queryParam("renderMethod", "SVG")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList<ReportDefinition>()
      .returnResult()

    assertThat(result.responseBody).isNotNull
    assertThat(result.responseBody).hasSize(0)
  }

  @Test
  fun `Definitions do not contain null values`() {
    val result = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/definitions")
          .queryParam("renderMethod", "HTML")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(String::class.java)
      .returnResult()

    assertThat(result.responseBody).isNotNull
    assertThat(result.responseBody).doesNotContain(": null")
  }
}
