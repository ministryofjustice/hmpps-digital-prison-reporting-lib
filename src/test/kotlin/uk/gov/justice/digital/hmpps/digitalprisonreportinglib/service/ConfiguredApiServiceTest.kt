package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.validation.ValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.Companion.EXTERNAL_MOVEMENTS_PRODUCT_ID
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.Filter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.FilterType.DATE_RANGE_END
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.FilterType.DATE_RANGE_START
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.FilterType.DYNAMIC
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.FilterTypeDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IsoLocalDateTimeTypeAdaptor
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.JsonFileProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.PolicyTypeDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RuleEffectTypeDeserializer
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.SchemaFieldTypeDeserializer

class ConfiguredApiServiceTest {
  private val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
    IsoLocalDateTimeTypeAdaptor(),
    listOf("productDefinition.json"),
    FilterTypeDeserializer(),
    SchemaFieldTypeDeserializer(),
    RuleEffectTypeDeserializer(),
    PolicyTypeDeserializer(),
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
  private val caseloads = listOf("WWI")
  private val caseloadFields = listOf("origin_code", "destination_code")

  private val reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows when both range and non range filters are provided`() {
    val reportVariantId = "last-month"
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
        caseloads,
        caseloadFields,
        reportId,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, caseloads)

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      repositoryFilters,
      selectedPage,
      pageSize,
      sortColumn,
      sortedAsc,
      caseloads,
      caseloadFields,
      reportId,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows when a dynamic filter is provided`() {
    val reportVariantId = "last-month"
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
        caseloads,
        caseloadFields,
        reportId,
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
      caseloads,
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
      caseloads,
      caseloadFields,
      reportId,
      reportFieldId,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows when both range and non range filters are provided`() {
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("direction", "in"), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(configuredApiRepository.count(repositoryFilters, dataSet.query, caseloads, caseloadFields, reportId)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters, caseloads)

    verify(configuredApiRepository, times(1)).count(
      repositoryFilters,
      dataSet.query,
      caseloads,
      caseloadFields,
      reportId,
    )
    assertEquals(Count(4), actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows when only range filters are provided`() {
    val reportVariantId = "last-month"
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
        caseloads,
        caseloadFields,
        reportId,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, caseloads)

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      repositoryFilters,
      selectedPage,
      pageSize,
      sortColumn,
      sortedAsc,
      caseloads,
      caseloadFields,
      reportId,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows when only range filters are provided`() {
    val reportVariantId = "last-month"
    val filters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(configuredApiRepository.count(repositoryFilters, dataSet.query, caseloads, caseloadFields, reportId)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters, caseloads)

    verify(configuredApiRepository, times(1)).count(
      repositoryFilters,
      dataSet.query,
      caseloads,
      caseloadFields,
      reportId,
    )
    assertEquals(Count(4), actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows when only non range filters are provided`() {
    val reportVariantId = "last-month"
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
        caseloads,
        caseloadFields,
        reportId,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filtersExcludingRange, selectedPage, pageSize, sortColumn, sortedAsc, caseloads)

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      repositoryFilters,
      selectedPage,
      pageSize,
      sortColumn,
      sortedAsc,
      caseloads,
      caseloadFields,
      reportId,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows when only non range filters are provided`() {
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "in")
    val repositoryFilters = listOf(Filter("direction", "in"))
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(configuredApiRepository.count(repositoryFilters, dataSet.query, caseloads, caseloadFields, reportId)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters, caseloads)

    verify(configuredApiRepository, times(1)).count(
      repositoryFilters,
      dataSet.query,
      caseloads,
      caseloadFields,
      reportId,
    )
    assertEquals(Count(4), actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows regardless of the casing of the values of the non range filters`() {
    val reportVariantId = "last-month"
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
        caseloads,
        caseloadFields,
        reportId,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, caseloads)

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      repositoryFilters,
      selectedPage,
      pageSize,
      sortColumn,
      sortedAsc,
      caseloads,
      caseloadFields,
      reportId,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows regardless of the casing of the values of the non range filters`() {
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "In", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("direction", "In"), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(configuredApiRepository.count(repositoryFilters, dataSet.query, caseloads, caseloadFields, reportId)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters, caseloads)

    verify(configuredApiRepository, times(1)).count(
      repositoryFilters,
      dataSet.query,
      caseloads,
      caseloadFields,
      reportId,
    )
    assertEquals(Count(4), actual)
  }

  @Test
  fun `the service calls the repository without filters if no filters are provided`() {
    val reportVariantId = "last-month"
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
        caseloads,
        caseloadFields,
        reportId,
      ),
    ).thenReturn(
      listOf(
        mapOf("PRISONNUMBER" to "1"),
      ),
    )

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc, caseloads)

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      emptyList(),
      1,
      10,
      "date",
      true,
      caseloads,
      caseloadFields,
      reportId,
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
    val reportVariantId = "last-month"
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()

    whenever(configuredApiRepository.count(emptyList(), dataSet.query, caseloads, caseloadFields, reportId)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, emptyMap(), caseloads)

    verify(configuredApiRepository, times(1)).count(emptyList(), dataSet.query, caseloads, caseloadFields, reportId)
    assertEquals(Count(4), actual)
  }

  @Test
  fun `validateAndFetchData should throw an exception for invalid report id`() {
    val reportId = "random report id"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, caseloads)
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
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception for invalid report id`() {
    val reportId = "random report id"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, caseloads)
    }
    assertEquals("${ConfiguredApiService.INVALID_REPORT_ID_MESSAGE} $reportId", e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any())
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
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, caseloads)
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
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception for invalid report variant`() {
    val reportVariantId = "non existent variant"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, caseloads)
    }
    assertEquals("${ConfiguredApiService.INVALID_REPORT_VARIANT_ID_MESSAGE} $reportVariantId", e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception for invalid sort column`() {
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "abc"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, caseloads)
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
      any(),
    )
  }

  @Test
  fun `validateAndFetchData should throw an exception for invalid filter`() {
    val reportVariantId = "last-month"
    val filters = mapOf("non existent filter" to "blah")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, caseloads)
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
      any(),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["origin", "invalid field name"])
  fun `validateAndFetchData should throw an exception for invalid dynamic filter`(fieldId: String) {
    val reportVariantId = "last-month"
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc, caseloads, fieldId, "ab")
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
      any(),
    )
  }

  @Test
  fun `validateAndFetchData should throw an exception for a fieldId which is a filter but not a dynamic one`() {
    val reportVariantId = "last-month"
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val fieldId = "direction"

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc, caseloads, fieldId, "ab")
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
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception for invalid filter`() {
    val reportVariantId = "last-month"
    val filters = mapOf("non existent filter" to "blah")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, caseloads)
    }
    assertEquals(ConfiguredApiService.INVALID_FILTERS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception when having a valid and an invalid filter`() {
    val reportVariantId = "last-month"
    val filters = mapOf("non existent filter" to "blah", "date$RANGE_FILTER_START_SUFFIX" to "2023-01-01")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, caseloads)
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
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception when having a valid and an invalid filter`() {
    val reportVariantId = "last-month"
    val filters = mapOf("non existent filter" to "blah", "date$RANGE_FILTER_START_SUFFIX" to "2023-01-01")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, caseloads)
    }
    assertEquals(ConfiguredApiService.INVALID_FILTERS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception when having invalid static options for a filter and a valid range filter`() {
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "randomValue", "date$RANGE_FILTER_START_SUFFIX" to "2023-01-01")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, caseloads)
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
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception when having invalid static options for a filter and a valid range filter`() {
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "randomValue", "date$RANGE_FILTER_START_SUFFIX" to "2023-01-01")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, caseloads)
    }
    assertEquals(ConfiguredApiService.INVALID_STATIC_OPTIONS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception when having invalid static options for a filter and no range filters`() {
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "randomValue")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, caseloads)
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
      any(),
    )
  }

  @Test
  fun `validateAndFetchData should throw an exception when having a prefix with fewer characters than the minimum length`() {
    val reportVariantId = "last-month"
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
        caseloads,
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
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception when having invalid static options for a filter and no range filters`() {
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "randomValue")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, caseloads)
    }
    assertEquals(ConfiguredApiService.INVALID_STATIC_OPTIONS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception when having an invalid range filter`() {
    val reportVariantId = "last-month"
    val filters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "abc")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, caseloads)
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
      any(),
    )
  }

  @Test
  fun `validateAndCount should throw an exception when having an invalid range filter`() {
    val reportVariantId = "last-month"
    val filters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "abc")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, caseloads)
    }
    assertEquals("Invalid value abc for filter date. Cannot be parsed as a date.", e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any())
  }

  @Test
  fun `should call the configuredApiRepository with the default sort column if none is provided`() {
    val reportVariantId = "last-month"
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
        caseloads,
        caseloadFields,
        reportId,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, null, sortedAsc, caseloads)

    verify(configuredApiRepository, times(1)).executeQuery(
      dataSet.query,
      repositoryFilters,
      selectedPage,
      pageSize,
      sortColumn,
      sortedAsc,
      caseloads,
      caseloadFields,
      reportId,
    )
    assertEquals(expectedServiceResult, actual)
  }
}
