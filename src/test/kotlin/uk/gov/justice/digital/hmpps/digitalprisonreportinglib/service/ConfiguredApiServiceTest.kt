package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.validation.ValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.Companion.EXTERNAL_MOVEMENTS_PRODUCT_ID
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.Filter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.FilterType.DATE_RANGE_END
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.FilterType.DATE_RANGE_START
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.FilterType.DYNAMIC
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IsoLocalDateTimeTypeAdaptor
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.JsonFileProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MetaData
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Schema
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Specification
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Effect
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_PERMIT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Rule
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import java.time.LocalDateTime

class ConfiguredApiServiceTest {
  private val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
    listOf("productDefinition.json"),
    DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
  )
  private val configuredApiRepository: ConfiguredApiRepository = mock<ConfiguredApiRepository>()
  private val configuredApiService = ConfiguredApiService(productDefinitionRepository, configuredApiRepository)
  private val expectedRepositoryResult = listOf(
    mapOf("PRISONNUMBER" to "1"),
    mapOf("NAME" to "FirstName"),
    mapOf("DATE" to "2023-05-20"),
    mapOf("ORIGIN" to "OriginLocation"),
    mapOf("DESTINATION" to "DestinationLocation"),
    mapOf("DIRECTION" to "in"),
    mapOf("TYPE" to "trn"),
    mapOf("REASON" to "normal transfer"),
  )
  private val expectedServiceResult = listOf(
    mapOf("prisonNumber" to "1"),
    mapOf("name" to "FirstName"),
    mapOf("date" to "2023-05-20"),
    mapOf("origin" to "OriginLocation"),
    mapOf("destination" to "DestinationLocation"),
    mapOf("direction" to "in"),
    mapOf("type" to "trn"),
    mapOf("reason" to "normal transfer"),
  )
  private val authToken = mock<DprAuthAwareAuthenticationToken>()
  private val reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID
  private val reportVariantId = "last-month"
  val policyEngineResult = "(origin_code='WWI' AND lower(direction)='out') OR (destination_code='WWI' AND lower(direction)='in')"

  @BeforeEach
  fun setup() {
    whenever(authToken.getCaseLoads()).thenReturn(listOf("WWI"))
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows when both range and non range filters are provided`() {
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("direction", "in"), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(
      configuredApiRepository.executeQuery(
        dataSet.query,
        repositoryFilters,
        selectedPage,
        pageSize,
        sortColumn,
        sortedAsc,
        reportId,
        policyEngineResult,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      repositoryFilters,
      selectedPage,
      pageSize,
      sortColumn,
      sortedAsc,
      reportId,
      policyEngineResult,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows when a dynamic filter is provided`() {
    val filters = mapOf(
      "direction" to "in",
      "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25",
      "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10",
    )
    val repositoryFilters = listOf(
      Filter("direction", "in"),
      Filter("date", "2023-04-25", DATE_RANGE_START),
      Filter("date", "2023-09-10", DATE_RANGE_END),
      Filter("name", "Ab", DYNAMIC),
    )
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()
    val reportFieldId = "name"
    val prefix = "Ab"
    whenever(
      configuredApiRepository.executeQuery(
        dataSet.query,
        repositoryFilters,
        selectedPage,
        pageSize,
        sortColumn,
        sortedAsc,
        reportId,
        policyEngineResult,
        reportFieldId,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(
      reportId,
      reportVariantId,
      filters,
      selectedPage,
      pageSize,
      sortColumn,
      sortedAsc,
      authToken,
      reportFieldId,
      prefix,
    )

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      repositoryFilters,
      selectedPage,
      pageSize,
      sortColumn,
      sortedAsc,
      reportId,
      policyEngineResult,
      reportFieldId,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows when both range and non range filters are provided`() {
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("direction", "in"), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(configuredApiRepository.count(repositoryFilters, dataSet.query, reportId, policyEngineResult)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)

    verify(configuredApiRepository, times(1)).count(
      repositoryFilters,
      dataSet.query,
      reportId,
      policyEngineResult,
    )
    assertEquals(Count(4), actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows when only range filters are provided`() {
    val filters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(
      configuredApiRepository.executeQuery(
        dataSet.query,
        repositoryFilters,
        selectedPage,
        pageSize,
        sortColumn,
        sortedAsc,
        reportId,
        policyEngineResult,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      repositoryFilters,
      selectedPage,
      pageSize,
      sortColumn,
      sortedAsc,
      reportId,
      policyEngineResult,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows when only range filters are provided`() {
    val filters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(configuredApiRepository.count(repositoryFilters, dataSet.query, reportId, policyEngineResult)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)

    verify(configuredApiRepository, times(1)).count(
      repositoryFilters,
      dataSet.query,
      reportId,
      policyEngineResult,
    )
    assertEquals(Count(4), actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows when only non range filters are provided`() {
    val filtersExcludingRange = mapOf("direction" to "in")
    val repositoryFilters = listOf(Filter("direction", "in"))

    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(
      configuredApiRepository.executeQuery(
        dataSet.query,
        repositoryFilters,
        selectedPage,
        pageSize,
        sortColumn,
        sortedAsc,
        reportId,
        policyEngineResult = policyEngineResult,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filtersExcludingRange, selectedPage, pageSize, sortColumn, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      repositoryFilters,
      selectedPage,
      pageSize,
      sortColumn,
      sortedAsc,
      reportId,
      policyEngineResult = policyEngineResult,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows when only non range filters are provided`() {
    val filters = mapOf("direction" to "in")
    val repositoryFilters = listOf(Filter("direction", "in"))
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(configuredApiRepository.count(repositoryFilters, dataSet.query, reportId, policyEngineResult)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)

    verify(configuredApiRepository, times(1)).count(
      repositoryFilters,
      dataSet.query,
      reportId,
      policyEngineResult,
    )
    assertEquals(Count(4), actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows regardless of the casing of the values of the non range filters`() {
    val filters = mapOf("direction" to "In", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("direction", "In"), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(
      configuredApiRepository.executeQuery(
        dataSet.query,
        repositoryFilters,
        selectedPage,
        pageSize,
        sortColumn,
        sortedAsc,
        reportId,
        policyEngineResult = policyEngineResult,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      repositoryFilters,
      selectedPage,
      pageSize,
      sortColumn,
      sortedAsc,
      reportId,
      policyEngineResult = policyEngineResult,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows regardless of the casing of the values of the non range filters`() {
    val filters = mapOf("direction" to "In", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("direction", "In"), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(configuredApiRepository.count(repositoryFilters, dataSet.query, reportId, policyEngineResult)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)

    verify(configuredApiRepository, times(1)).count(
      repositoryFilters,
      dataSet.query,
      reportId,
      policyEngineResult,
    )
    assertEquals(Count(4), actual)
  }

  @Test
  fun `the service calls the repository without filters if no filters are provided`() {
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    whenever(
      configuredApiRepository.executeQuery(
        dataSet.query,
        emptyList(),
        selectedPage,
        pageSize,
        sortColumn,
        sortedAsc,
        reportId,
        policyEngineResult = policyEngineResult,
      ),
    ).thenReturn(
      listOf(
        mapOf("PRISONNUMBER" to "1"),
      ),
    )

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      emptyList(),
      1,
      10,
      "date",
      true,
      reportId,
      policyEngineResult = policyEngineResult,
    )
    assertEquals(
      listOf(
        mapOf("prisonNumber" to "1"),
      ),
      actual,
    )
  }

  @Test
  fun `the service count method calls the repository without filters if no filters are provided`() {
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(configuredApiRepository.count(emptyList(), dataSet.query, reportId, policyEngineResult)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, emptyMap(), authToken)

    verify(configuredApiRepository, times(1)).count(emptyList(), dataSet.query, reportId, policyEngineResult)
    assertEquals(Count(4), actual)
  }

  @Test
  fun `validateAndFetchData should throw an exception for invalid report id`() {
    val reportId = "random report id"

    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)
    }
    assertEquals("${ConfiguredApiService.INVALID_REPORT_ID_MESSAGE} $reportId", e.message)
    verify(configuredApiRepository, times(0)).executeQuery(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception for invalid report id`() {
    val reportId = "random report id"

    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)
    }
    assertEquals("${ConfiguredApiService.INVALID_REPORT_ID_MESSAGE} $reportId", e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception for invalid report variant`() {
    val reportVariantId = "non existent variant"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)
    }
    assertEquals("${ConfiguredApiService.INVALID_REPORT_VARIANT_ID_MESSAGE} $reportVariantId", e.message)
    verify(configuredApiRepository, times(0)).executeQuery(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception for invalid report variant`() {
    val reportVariantId = "non existent variant"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)
    }
    assertEquals("${ConfiguredApiService.INVALID_REPORT_VARIANT_ID_MESSAGE} $reportVariantId", e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception for invalid sort column`() {
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "abc"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)
    }
    assertEquals("Invalid sortColumn provided: abc", e.message)
    verify(configuredApiRepository, times(0)).executeQuery(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
    )
  }

  @Test
  fun `validateAndFetchData should throw an exception for invalid filter`() {
    val filters = mapOf("non existent filter" to "blah")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)
    }
    assertEquals(ConfiguredApiService.INVALID_FILTERS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).executeQuery(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["origin", "invalid field name"])
  fun `validateAndFetchData should throw an exception for invalid dynamic filter`(fieldId: String) {
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc, authToken, fieldId, "ab")
    }
    assertEquals(ConfiguredApiService.INVALID_FILTERS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).executeQuery(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
    )
  }

  @Test
  fun `validateAndFetchData should throw an exception for a fieldId which is a filter but not a dynamic one`() {
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val fieldId = "direction"

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc, authToken, fieldId, "ab")
    }
    assertEquals(ConfiguredApiService.INVALID_DYNAMIC_FILTER_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).executeQuery(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception for invalid filter`() {
    val filters = mapOf("non existent filter" to "blah")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)
    }
    assertEquals(ConfiguredApiService.INVALID_FILTERS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception when having a valid and an invalid filter`() {
    val filters = mapOf("non existent filter" to "blah", "date$RANGE_FILTER_START_SUFFIX" to "2023-01-01")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)
    }
    assertEquals(ConfiguredApiService.INVALID_FILTERS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).executeQuery(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception when having a valid and an invalid filter`() {
    val filters = mapOf("non existent filter" to "blah", "date$RANGE_FILTER_START_SUFFIX" to "2023-01-01")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)
    }
    assertEquals(ConfiguredApiService.INVALID_FILTERS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception when having invalid static options for a filter and a valid range filter`() {
    val filters = mapOf("direction" to "randomValue", "date$RANGE_FILTER_START_SUFFIX" to "2023-01-01")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)
    }
    assertEquals(ConfiguredApiService.INVALID_STATIC_OPTIONS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).executeQuery(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception when having invalid static options for a filter and a valid range filter`() {
    val filters = mapOf("direction" to "randomValue", "date$RANGE_FILTER_START_SUFFIX" to "2023-01-01")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)
    }
    assertEquals(ConfiguredApiService.INVALID_STATIC_OPTIONS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception when having invalid static options for a filter and no range filters`() {
    val filters = mapOf("direction" to "randomValue")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)
    }
    assertEquals(ConfiguredApiService.INVALID_STATIC_OPTIONS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).executeQuery(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
    )
  }

  @Test
  fun `validateAndFetchData should throw an exception when having a prefix with fewer characters than the minimum length`() {
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(
        reportId,
        reportVariantId,
        mapOf(),
        selectedPage,
        pageSize,
        sortColumn,
        sortedAsc,
        authToken,
        "name",
        "A",
      )
    }
    assertEquals(ConfiguredApiService.INVALID_DYNAMIC_OPTIONS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).executeQuery(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception when having invalid static options for a filter and no range filters`() {
    val filters = mapOf("direction" to "randomValue")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)
    }
    assertEquals(ConfiguredApiService.INVALID_STATIC_OPTIONS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception when having an invalid range filter`() {
    val filters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "abc")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)
    }
    assertEquals("Invalid value abc for filter date. Cannot be parsed as a date.", e.message)
    verify(configuredApiRepository, times(0)).executeQuery(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception when having an invalid range filter`() {
    val filters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "abc")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)
    }
    assertEquals("Invalid value abc for filter date. Cannot be parsed as a date.", e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any())
  }

  @Test
  fun `should call the configuredApiRepository with the default sort column if none is provided`() {
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("direction", "in"), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(
      configuredApiRepository.executeQuery(
        dataSet.query,
        repositoryFilters,
        selectedPage,
        pageSize,
        sortColumn,
        sortedAsc,
        reportId,
        policyEngineResult,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, null, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      repositoryFilters,
      selectedPage,
      pageSize,
      sortColumn,
      sortedAsc,
      reportId,
      policyEngineResult,
    )
    assertEquals(expectedServiceResult, actual)
  }
  companion object {
  }

  @Test
  fun `should call the configuredApiRepository with no sort column if none is provided and there is no default`() {
    val dataSet =
      Dataset("datasetId", "datasetname", "select *", Schema(listOf(SchemaField("9", ParameterType.String))))
    val report = Report(
      id = "6",
      name = "7",
      created = LocalDateTime.MAX,
      version = "8",
      dataset = "\$ref:datasetId",
      render = RenderMethod.SVG,
      classification = "someClassification",
      specification = Specification(
        "list",
        listOf(
          ReportField(
            name = "\$ref:9",
            display = "Number 9",
            formula = "",
            visible = true,
            sortable = true,
            defaultSort = false,
          ),
        ),
      ),
    )
    val policy = Policy(
      "caseload",
      PolicyType.ROW_LEVEL,
      listOf("TRUE"),
      listOf(Rule(Effect.PERMIT, emptyList())),
    )
    val productDefinition = ProductDefinition(
      id = "1",
      name = "2",
      metadata = MetaData(
        author = "3",
        owner = "4",
        version = "5",
      ),
      policy = listOf(policy),
      dataset = listOf(dataSet),
      report = listOf(
        report,
      ),
    )
    val expectedRepositoryResult = listOf(
      mapOf("9" to "1"),
    )
    val expectedServiceResult = listOf(
      mapOf("9" to "1"),
    )
    val productDefRepo = mock<ProductDefinitionRepository>()
    val configuredApiService = ConfiguredApiService(productDefRepo, configuredApiRepository)
    whenever(productDefRepo.getProductDefinitions())
      .thenReturn(listOf(productDefinition))
    whenever(productDefRepo.getSingleReportProductDefinition(reportId, reportVariantId))
      .thenReturn(
        SingleReportProductDefinition(
          id = "1",
          name = "2",
          metadata = MetaData(
            author = "3",
            owner = "4",
            version = "5",
          ),
          policy = listOf(policy),
          dataset = dataSet,
          report = report,
          datasource = Datasource("id", "name"),
        ),
      )

    val selectedPage = 1L
    val pageSize = 10L
    val sortedAsc = true

    whenever(
      configuredApiRepository.executeQuery(
        dataSet.query,
        emptyList(),
        selectedPage,
        pageSize,
        null,
        sortedAsc,
        reportId,
        POLICY_PERMIT,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, emptyMap(), selectedPage, pageSize, null, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      emptyList(),
      selectedPage,
      pageSize,
      null,
      sortedAsc,
      reportId,
      POLICY_PERMIT,
    )
    assertEquals(expectedServiceResult, actual)
  }
}
