package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.StatusAssertions
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.container.PostgresContainer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.FILTERS_PREFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ResponseHeader
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.DATE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.DESTINATION
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.DESTINATION_CODE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.DIRECTION
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.NAME
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.ORIGIN
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.ORIGIN_CODE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.REASON
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.TYPE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisoner4
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovementPrisoners.movementPrisonerDestinationCaseloadDirectionIn
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovement4
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovementDestinationCaseloadDirectionIn
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ExternalMovementEntity
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.PrisonerEntity
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.WARNING_NO_ACTIVE_CASELOAD
import uk.gov.justice.hmpps.kotlin.auth.AuthSource
import java.sql.DriverManager
import java.time.LocalDateTime

class DataApiIntegrationTest : IntegrationTestBase() {
  companion object {
    fun dateTimeWithSeconds(dateTime: Any?) = """$dateTime:00"""

    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
    }
  }

  val caseloadsWithNoneActive: String = """
          {
            "username": "TESTUSER1",
            "active": true,
            "accountType": "GENERAL",
            "caseloads": [
              {
                "id": "WWI",
                "name": "WANDSWORTH (HMP)"
              }
            ]
          }
  """.trimIndent()

  /**
   * Nested test class so we can add a couple entries to the mock db just for these tests
   */
  @Nested
  inner class SortDirectionDataApiIntegrationTest {
    @BeforeEach
    fun init() {
      externalMovementRepository.save(externalMovementDestinationCaseloadDirectionIn)
      prisonerRepository.save(PrisonerEntity(9848, "DD105GF", "FirstName6", "LastName6", null))
    }

    @Test
    fun `Data API returns value using defaultSort on DPD with sortedAsc but no column`() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/reports/external-movements/last-month")
            .queryParam("selectedPage", 1)
            .queryParam("pageSize", 3)
            .queryParam("sortedAsc", true)
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.[0].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisoner4[DATE]))
        .jsonPath("$.[1].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisonerDestinationCaseloadDirectionIn[DATE]))
    }

    @Test
    fun `Data API returns value using defaultSort on DPD with column override but no sortedAsc`() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/reports/external-movements/last-month")
            .queryParam("selectedPage", 1)
            .queryParam("pageSize", 3)
            .queryParam("sortColumn", "date")
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.[0].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisoner4[DATE]))
        .jsonPath("$.[1].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisonerDestinationCaseloadDirectionIn[DATE]))
    }

    @Test
    fun `Data API returns value using defaultSort on DPD with no column override and no sortDirection`() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/reports/external-movements/last-month")
            .queryParam("selectedPage", 1)
            .queryParam("pageSize", 3)
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.[0].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisoner4[DATE]))
        .jsonPath("$.[1].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisonerDestinationCaseloadDirectionIn[DATE]))
    }

    @Test
    fun `Data API returns value using defaultSort on DPD but with an override`() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/reports/external-movements/last-month")
            .queryParam("selectedPage", 1)
            .queryParam("pageSize", 3)
            .queryParam("sortColumn", "date")
            .queryParam("sortedAsc", true)
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.[0].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisoner4[DATE]))
        .jsonPath("$.[1].date")
        .isEqualTo(dateTimeWithSeconds(movementPrisonerDestinationCaseloadDirectionIn[DATE]))
    }
  }

  class RolePolicyTestExternalUser : IntegrationTestBase() {
    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinitionWithRoleAndLaoPolicy.json" }
        registry.add("dpr.lib.user.requiredAuthSources") { "DELIUS,AUTH" }
        registry.add("dpr.lib.hasProbationDatasources") { true }
      }
    }

    @Test
    fun `Data API returns value when an external user has the correct roles`() {
      manageUsersMockServer.stubLookupUserCaseload(activeCaseloadId = "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(authSource = AuthSource.AUTH)
      manageUsersMockServer.stubLookupUsersRoles(roles = listOf("INCIDENT_REPORTS__RO", "PRISONS_REPORTING_USER"))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("5")
    }
  }

  @Test
  fun `Data API returns value from the repository`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month")
          .queryParam("selectedPage", 1)
          .queryParam("pageSize", 3)
          .queryParam("sortColumn", "date")
          .queryParam("sortedAsc", false)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(
        """[
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "${dateTimeWithSeconds(movementPrisoner4[DATE])}", 
        "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN_CODE]}", "destination": "${movementPrisoner4[DESTINATION]}", "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
        "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ]       
      """,
      )
  }

  @Test
  fun `Data API is streaming results to the downloaded csv file`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month/download")
          .queryParam("columns", "prisonNumber")
          .queryParam("columns", "name")
          .queryParam("columns", "origin")
          .queryParam("columns", "destination")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader().contentType("text/csv")
      .expectHeader().valueEquals(
        "Content-Disposition",
        "attachment; filename=external-movements-last-month.csv",
      )
      .expectBody(String::class.java)
      .value { body ->
        val expected = """
          Prison Number,Name,From,To
          ${movementPrisoner4[PRISON_NUMBER]},"${movementPrisoner4[NAME]}",${movementPrisoner4[ORIGIN]},${movementPrisoner4[DESTINATION]}
        """.trimIndent()
        assertThat(body?.startsWith("\uFEFF")).isTrue()
        assertThat(body?.trim()?.replace("\uFEFF", "")).isEqualTo(expected)
      }
  }

  class ConfiguredApiFormulaTest : IntegrationTestBase() {
    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinitionWithFormula.json" }
        registry.add("URL_ENV_SUFFIX") { "dev" }
      }
    }

    @Test
    fun `Data API returns value from the repository formatting the fields with formulas correctly`() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/reports/external-movements/last-month")
            .queryParam("selectedPage", 1)
            .queryParam("pageSize", 3)
            .queryParam("sortColumn", "date")
            .queryParam("sortedAsc", false)
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json(
          """[
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "01/05/2023", 
        "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN]}", 
        "destination": "<a href='https://prisoner-dev.digital.prison.service.justice.gov.uk/prisoner/${movementPrisoner4[PRISON_NUMBER]}' class=\"govuk-link\" rel=\"noreferrer noopener\" target=\"_blank\">${movementPrisoner4[NAME]}</a>", 
        "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
        "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ]       
      """,
        )
    }

    @Test
    fun `Data API returns empty String as the value from the repository for a field which has a formula and whose result set value is null`() {
      externalMovementRepository.delete(externalMovement4)
      val externalMovement4WithNullType = ExternalMovementEntity(
        4,
        7849,
        LocalDateTime.of(2023, 5, 1, 0, 0, 0),
        LocalDateTime.of(2023, 5, 1, 15, 19, 0),
        null,
        "LWSTMC",
        "WANDSWORTH (HMP)",
        "WWI",
        "Out",
        "Transfer",
        "Transfer Out to Other Establishment",
      )
      externalMovementRepository.save(externalMovement4WithNullType)
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/reports/external-movements/last-month")
            .queryParam("selectedPage", 1)
            .queryParam("pageSize", 3)
            .queryParam("sortColumn", "date")
            .queryParam("sortedAsc", false)
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json(
          """[
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "01/05/2023", 
        "origin": null, "origin_code": "", 
        "destination": "<a href='https://prisoner-dev.digital.prison.service.justice.gov.uk/prisoner/${movementPrisoner4[PRISON_NUMBER]}' class=\"govuk-link\" rel=\"noreferrer noopener\" target=\"_blank\">${movementPrisoner4[NAME]}</a>", 
        "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
        "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ]       
      """,
        )
    }

    @Test
    fun `Data API returns value from the repository applying formulas correctly to the dashboard result dataset`() {
      webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/reports/external-movements/dashboards/age-breakdown-dashboard-1")
            .queryParam("sortColumn", "date")
            .queryParam("sortedAsc", false)
            .build()
        }
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json(
          """
        [
          {
            "prisonNumber": {
              "raw": "G3411VR"
            },
            "name": {
              "raw": "LastName5, F"
            },
            "date": {
              "raw": "2023-05-01T15:19:00"
            },
            "direction": {
              "raw": "Out"
            },
            "type": {
              "raw": "Transfer"
            },
            "origin": {
              "raw": "Lowestoft (North East Suffolk) Magistrat"
            },
            "origin_code": {
              "raw": "LWSTMC"
            },
            "destination": {
              "raw": "<a href='https://prisoner-dev.digital.prison.service.justice.gov.uk/prisoner/G3411VR' class=\"govuk-link\" rel=\"noreferrer noopener\" target=\"_blank\">LastName5, F</a>"
            },
            "destination_code": {
              "raw": "WWI"
            },
            "reason": {
              "raw": "Transfer Out to Other Establishment"
            }
          }
        ]
      """,
        )
    }
  }

  @Test
  fun `Data API count returns the number of records`() {
    webTestClient.get()
      .uri("/reports/external-movements/last-month/count")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("count").isEqualTo("1")
  }

  @ParameterizedTest
  @CsvSource(
    "In,  0",
    "Out, 1",
    ",    1",
  )
  fun `Data API count returns filtered value`(direction: String?, numberOfResults: Int) {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month/count")
          .queryParam("filters.direction", direction?.lowercase() ?: "")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .jsonPath("count").isEqualTo(numberOfResults.toString())
  }

  @Test
  fun `Data API returns value matching the filters provided`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month")
          .queryParam("${FILTERS_PREFIX}date$RANGE_FILTER_START_SUFFIX", "2023-04-25")
          .queryParam("${FILTERS_PREFIX}date$RANGE_FILTER_END_SUFFIX", "2023-05-20")
          .queryParam("${FILTERS_PREFIX}direction", "out")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .json(
        """[
         {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "${dateTimeWithSeconds(movementPrisoner4[DATE])}",
          "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN_CODE]}", "destination": "${movementPrisoner4[DESTINATION]}", "destination_code": "${movementPrisoner4[DESTINATION_CODE]}", 
          "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ]       
      """,
      )
  }

  @Test
  fun `Data API call without query params defaults to preset query params`() {
    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        """
      [
        {"prisonNumber": "${movementPrisoner4[PRISON_NUMBER]}", "name": "${movementPrisoner4[NAME]}", "date": "${dateTimeWithSeconds(movementPrisoner4[DATE])}", 
        "origin": "${movementPrisoner4[ORIGIN]}", "origin_code": "${movementPrisoner4[ORIGIN_CODE]}", "destination": "${movementPrisoner4[DESTINATION]}", "destination_code": "${movementPrisoner4[DESTINATION_CODE]}",
        "direction": "${movementPrisoner4[DIRECTION]}", "type": "${movementPrisoner4[TYPE]}", "reason": "${movementPrisoner4[REASON]}"}
      ]
      """,
      )
  }

  @ParameterizedTest
  @CsvSource(
    "in,  0",
    "In,  0",
    "out, 1",
    "Out, 1",
    ",    1",
  )
  fun `Data API returns filtered values`(direction: String?, numberOfResults: Int) {
    val results = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month")
          .queryParam("${FILTERS_PREFIX}direction", direction ?: "")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBodyList<Map<String, Any>>()
      .hasSize(numberOfResults)
      .returnResult()
      .responseBody

    if (direction != null) {
      results?.forEach {
        assertThat(it["direction"].toString().lowercase()).isEqualTo(direction.lowercase())
      }
    }
  }

  @Test
  fun `Data API returns empty list and warning header if no active caseloads`() {
    wireMockServer.resetAll()
    manageUsersMockServer.stubLookupUserCaseload("request-user", null)
    manageUsersMockServer.stubGetUserInfo("request-user")
    stubDefinitionsResponse()

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month")
          .queryParam("selectedPage", 1)
          .queryParam("pageSize", 3)
          .queryParam("sortColumn", "date")
          .queryParam("sortedAsc", false)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .valueEquals(ResponseHeader.NO_DATA_WARNING_HEADER_NAME, listOf(WARNING_NO_ACTIVE_CASELOAD).toString())
      .expectBody()
      .json("[]")
  }

  @Test
  fun `Data API returns empty list and no warning header if no active caseloads and not NOMIS user`() {
    wireMockServer.resetAll()
    manageUsersMockServer.stubLookupUserCaseload("request-user", null)
    manageUsersMockServer.stubGetUserInfo("request-user", AuthSource.DELIUS)
    stubDefinitionsResponse()

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/reports/external-movements/last-month")
          .queryParam("selectedPage", 1)
          .queryParam("pageSize", 3)
          .queryParam("sortColumn", "date")
          .queryParam("sortedAsc", false)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .doesNotExist(ResponseHeader.NO_DATA_WARNING_HEADER_NAME)
      .expectBody()
      .json("[]")
  }

  @Test
  fun `Data API count returns zero and warning header if no active caseloads`() {
    manageUsersMockServer.stubLookupUserCaseload("request-user", null)
    manageUsersMockServer.stubGetUserInfo("request-user")
    stubDefinitionsResponse()

    webTestClient.get()
      .uri("/reports/external-movements/last-month/count")
      .headers(setAuthorisation(roles = listOf(authorisedRole)))
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .valueEquals(ResponseHeader.NO_DATA_WARNING_HEADER_NAME, listOf(WARNING_NO_ACTIVE_CASELOAD).toString())
      .expectBody()
      .jsonPath("count").isEqualTo("0")
  }

  class SentencePlanFederatedDatasourceTest : IntegrationTestBase() {
    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinitionWithSentencePlanFederatedDatasource.json" }
      }
    }

    @Test
    fun `Data API count returns forbidden if using sentence plan federated datasource`() {
      manageUsersMockServer.stubLookupUserCaseload("request-user")
      manageUsersMockServer.stubGetUserInfo("request-user")
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isForbidden()
    }
  }

  class SentencePlanTablesDatasourceQueryTest : IntegrationTestBase() {
    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinitionWithSentencePlanTablesInQuery.json" }
      }
    }

    @Test
    fun `Data API count returns forbidden if using sentence plan tables in normal datasource`() {
      manageUsersMockServer.stubLookupUserCaseload("request-user")
      manageUsersMockServer.stubGetUserInfo("request-user")
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isForbidden()
    }
  }

  class ProbationDataSourcesAuthSourceCaseloadPolicyTest : IntegrationTestBase() {
    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinitionWithLaoPermitPolicy.json" }
        registry.add("dpr.lib.hasProbationDatasources") { true }
        registry.add("dpr.lib.user.requiredAuthSources") { "DELIUS,AUTH" }
      }
    }

    @BeforeEach
    fun laoSetup() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("TRUNCATE TABLE product_.lao_exclusions; TRUNCATE TABLE product_.lao_restrictions; TRUNCATE TABLE product_.lao_crns;").execute()
    }

    @Test
    fun `should execute a report with a row level caseload policy as a probation user but return nothing`() {
      manageUsersMockServer.stubLookupUserCaseload404("request-user")
      manageUsersMockServer.stubGetUserInfo(authSource = AuthSource.DELIUS)
      manageUsersMockServer.stubLookupUsersRoles("request-user", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(authSource = AuthSource.DELIUS, roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("0")
    }

    @Test
    fun `should execute a report with a row level caseload policy as an external user but return nothing`() {
      manageUsersMockServer.stubLookupUserCaseload404("request-user")
      manageUsersMockServer.stubGetUserInfo(authSource = AuthSource.AUTH)
      manageUsersMockServer.stubLookupUsersRoles("request-user", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(authSource = AuthSource.AUTH, roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("0")
    }
  }

  class LaoDataApiIntegrationTestPermitPolicy : IntegrationTestBase() {
    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinitionWithLaoPermitPolicy.json" }
        registry.add("dpr.lib.hasProbationDatasources") { true }
        registry.add("dpr.lib.user.requiredAuthSources") { "DELIUS,AUTH" }
      }
    }

    @BeforeEach
    fun laoSetup() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("TRUNCATE TABLE product_.lao_exclusions; TRUNCATE TABLE product_.lao_restrictions; TRUNCATE TABLE product_.lao_crns;").execute()
    }

    @Test
    fun `Should return data if LAO permit policy exists even if user is excluded`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('G3411VR', 0, NOW())").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_exclusions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:P111111', 'G3411VR', 'P111111', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(username = "P111111", authSource = AuthSource.DELIUS)
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("1")
    }

    @Test
    fun `Should return data if LAO permit policy exists even if other user is restricted to LAO and requesting user is not`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('G3411VR', 0, NOW())").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_restrictions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:Z000000', 'G3411VR', 'Z000000', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(username = "P111111", authSource = AuthSource.DELIUS)
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("1")
    }
  }

  class LaoDataApiIntegrationTestPermitPolicyWithAction : IntegrationTestBase() {
    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinitionWithLaoPermitPolicyAndAction.json" }
        registry.add("dpr.lib.hasProbationDatasources") { true }
        registry.add("dpr.lib.user.requiredAuthSources") { "DELIUS,AUTH" }
      }
    }

    @BeforeEach
    fun laoSetup() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("TRUNCATE TABLE product_.lao_exclusions; TRUNCATE TABLE product_.lao_restrictions; TRUNCATE TABLE product_.lao_crns;")
        .execute()
    }

    @Test
    fun `Should return data if LAO permit policy exists even if user is excluded`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('G3411VR', 0, NOW())")
        .execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_exclusions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:P111111', 'G3411VR', 'P111111', 'a reason', NOW(), NOW() + INTERVAL '1 day')")
        .execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(username = "P111111", authSource = AuthSource.DELIUS)
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("1")
    }
  }

  class LaoDataApiIntegrationTestNoPolicy : IntegrationTestBase() {
    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinition.json" }
        registry.add("dpr.lib.hasProbationDatasources") { true }
      }
    }

    @BeforeEach
    fun laoSetup() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("TRUNCATE TABLE product_.lao_exclusions; TRUNCATE TABLE product_.lao_restrictions; TRUNCATE TABLE product_.lao_crns;").execute()
    }

    @Test
    fun `Should be forbidden if no LAO policy exists but running app with probation datasources`() {
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo("P111111")
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  class LaoDataApiIntegrationTest : IntegrationTestBase() {
    companion object {
      @JvmStatic
      @DynamicPropertySource
      fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("dpr.lib.definition.locations") { "productDefinitionWithLaoPolicy.json" }
        registry.add("dpr.lib.hasProbationDatasources") { true }
        registry.add("dpr.lib.user.requiredAuthSources") { "DELIUS,AUTH" }
      }
    }

    @BeforeEach
    fun laoSetup() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("TRUNCATE TABLE product_.lao_exclusions; TRUNCATE TABLE product_.lao_restrictions; TRUNCATE TABLE product_.lao_crns;").execute()
    }

    @Test
    fun `Data API count returns zero if LAO is restricted to someone who isnt the requesting user`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('G3411VR', 0, NOW())").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_restrictions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:Z000000', 'G3411VR', 'Z000000', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(username = "P111111", authSource = AuthSource.DELIUS)
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("0")
    }

    @Test
    fun `Data API count returns zero if requesting user is excluded from the LAO`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('G3411VR', 0, NOW())").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_exclusions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:P111111', 'G3411VR', 'P111111', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_exclusions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:P111115', 'G3411VR', 'P111115', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(username = "P111111", authSource = AuthSource.DELIUS)
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("0")
    }

    @Test
    fun `Data API count returns data if LAO is restricted to someone who isnt the requesting user but restriction is expired`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('G3411VR', 0, NOW())").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_restrictions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:Z000000', 'G3411VR', 'Z000000', 'a reason', NOW() - INTERVAL '2 day', NOW() - INTERVAL '1 day')").execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(username = "P111111", authSource = AuthSource.DELIUS)
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("1")
    }

    @Test
    fun `Data API count returns data if requesting user is excluded from the LAO but exclusion is expired`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('G3411VR', 0, NOW())").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_exclusions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:P111111', 'G3411VR', 'P111111', 'a reason', NOW() - INTERVAL '2 day', NOW() - INTERVAL '1 day')").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_exclusions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:P111115', 'G3411VR', 'P111115', 'a reason', NOW() - INTERVAL '2 day', NOW() - INTERVAL '1 day')").execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(username = "P111111", authSource = AuthSource.DELIUS)
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("1")
    }

    @Test
    fun `Data API count returns data if requesting user is not excluded from the LAO`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('G3411VR', 0, NOW())").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_exclusions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:P111112', 'G3411VR', 'P111112', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(username = "P111111", authSource = AuthSource.DELIUS)
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("1")
    }

    @Test
    fun `Data API count returns data if LAO is restricted to the requesting user`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('G3411VR', 0, NOW())").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_restrictions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:P111111', 'G3411VR', 'P111111', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_restrictions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:P111113', 'G3411VR', 'P111113', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(username = "P111111", authSource = AuthSource.DELIUS)
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("1")
    }

    @Test
    fun `Data API count returns zero if requesting user is restricted to AND excluded from the LAO`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('G3411VR', 0, NOW())").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_restrictions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:P111111', 'G3411VR', 'P111111', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_exclusions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:P111111', 'G3411VR', 'P111111', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(username = "P111111", authSource = AuthSource.DELIUS)
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("0")
    }

    @Test
    fun `Data API count returns data if data does not contain any LAOs`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('A123456', 0, NOW())").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_restrictions (crn_user_id, crn, user_id, reason, since, until) VALUES ('A123456:P111111', 'A123456', 'P111111', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_exclusions (crn_user_id, crn, user_id, reason, since, until) VALUES ('A123456:P111111', 'A123456', 'P111111', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(username = "P111111", authSource = AuthSource.DELIUS)
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("1")
    }

    @Test
    fun `Data API count returns data if requesting user is not excluded from the LAO and is an external user`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('G3411VR', 0, NOW())").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_exclusions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:P111112', 'G3411VR', 'P111112', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(authSource = AuthSource.AUTH, username = "P111111")
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole), authSource = AuthSource.AUTH))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("1")
    }

    @Test
    fun `Data API count returns data if data does not contain any LAOs and is an external user`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('A123456', 0, NOW())").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_restrictions (crn_user_id, crn, user_id, reason, since, until) VALUES ('A123456:P111111', 'A123456', 'P111111', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_exclusions (crn_user_id, crn, user_id, reason, since, until) VALUES ('A123456:P111111', 'A123456', 'P111111', 'a reason', NOW(), NOW() + INTERVAL '1 day')").execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(authSource = AuthSource.AUTH, username = "P111111")
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole), authSource = AuthSource.AUTH))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("1")
    }

    @Test
    fun `Data API count returns zero if LAO is restricted to someone who isnt the requesting user and the user is an external user`() {
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_crns (crn, version, last_updated) VALUES ('G3411VR', 0, NOW())")
        .execute()
      DriverManager.getConnection(PostgresContainer.jdbcUrl, "test", "test")
        .prepareStatement("INSERT INTO product_.lao_restrictions (crn_user_id, crn, user_id, reason, since, until) VALUES ('G3411VR:Z000000', 'G3411VR', 'Z000000', 'a reason', NOW(), NOW() + INTERVAL '1 day')")
        .execute()
      manageUsersMockServer.stubLookupUserCaseload("P111111", "LWSTMC")
      manageUsersMockServer.stubGetUserInfo(authSource = AuthSource.AUTH, username = "P111111")
      manageUsersMockServer.stubLookupUsersRoles("P111111", listOf(authorisedRole))
      stubDefinitionsResponse()

      webTestClient.get()
        .uri("/reports/external-movements/last-month/count")
        .headers(setAuthorisation(user = "P111111", roles = listOf(authorisedRole)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("count").isEqualTo("0")
    }
  }

  @Test
  fun `Data API returns 400 for invalid selectedPage query param`() {
    requestWithQueryAndAssert400("selectedPage", 0, "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API returns 400 for invalid pageSize query param`() {
    requestWithQueryAndAssert400("pageSize", 0, "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API returns 400 for invalid (wrong type) pageSize query param`() {
    requestWithQueryAndAssert400("pageSize", "a", "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API returns 400 for invalid sortColumn query param`() {
    requestWithQueryAndAssert400("sortColumn", "nonExistentColumn", "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API returns 400 for invalid sortedAsc query param`() {
    requestWithQueryAndAssert400("sortedAsc", "abc", "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API returns 400 for non-existent filter`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}abc", "abc", "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API count returns 400 for non-existent filter`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}abc", "abc", "/reports/external-movements/last-month/count")
  }

  @Test
  fun `Data API returns 400 for a report field which is not a filter`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}destination", "some name", "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API count returns 400 for a report field which is not a filter`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}destination", "some name", "/reports/external-movements/last-month/count")
  }

  @Test
  fun `Data API returns 400 for invalid startDate query param`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}date$RANGE_FILTER_START_SUFFIX", "abc", "/reports/external-movements/last-month")
  }

  @Test
  fun `External movements returns 400 for invalid endDate query param`() {
    requestWithQueryAndAssert400("${FILTERS_PREFIX}date$RANGE_FILTER_END_SUFFIX", "b", "/reports/external-movements/last-month")
  }

  @Test
  fun `Data API count returns 400 for invalid startDate query param`() {
    requestWithQueryAndAssert400("filters.startDate", "a", "/reports/external-movements/last-month/count")
  }

  @Test
  fun `Data API count returns 400 for invalid endDate query param`() {
    requestWithQueryAndAssert400("filters.endDate", "17-12-2050", "/reports/external-movements/last-month/count")
  }

  @Test
  fun `External movements returns 400 for missing mandatory filter query param`() {
    val params = mapOf(
      "${FILTERS_PREFIX}origin" to "AAA",
      "${FILTERS_PREFIX}date$RANGE_FILTER_END_SUFFIX" to "",
    )
    requestWithQueryAndAssert400(params, "/reports/external-movements/last-year")
  }

  @Test
  fun `External movements returns 400 for filter query param that doesn't match pattern`() {
    val params = mapOf(
      "${FILTERS_PREFIX}date$RANGE_FILTER_END_SUFFIX" to "2000-01-02",
      "${FILTERS_PREFIX}origin" to "Invalid",
    )
    requestWithQueryAndAssert400(params, "/reports/external-movements/last-year")
  }

  @Test
  fun `External movements returns 200 for filter query param that matches pattern`() {
    val params = mapOf(
      "${FILTERS_PREFIX}date$RANGE_FILTER_END_SUFFIX" to "2000-01-02",
      "${FILTERS_PREFIX}origin" to "AAA",
    )
    requestWithQueryAndAssert200(params, "/reports/external-movements/last-year")
  }

  private fun requestWithQueryAndAssert400(paramName: String, paramValue: Any, path: String) {
    requestWithQueryAndAssert400(mapOf(paramName to paramValue), path)
  }

  private fun requestWithQueryAndAssert400(params: Map<String, Any>, path: String) {
    requestWithQueryAndAssert(params, path).isBadRequest
  }

  private fun requestWithQueryAndAssert200(params: Map<String, Any>, path: String) {
    requestWithQueryAndAssert(params, path).isOk
  }

  private fun requestWithQueryAndAssert(params: Map<String, Any>, path: String): StatusAssertions = webTestClient.get()
    .uri { uriBuilder: UriBuilder ->
      params.entries.forEach { uriBuilder.queryParam(it.key, it.value) }

      uriBuilder.path(path).build()
    }
    .headers(setAuthorisation(roles = listOf(authorisedRole)))
    .exchange()
    .expectStatus()
}
