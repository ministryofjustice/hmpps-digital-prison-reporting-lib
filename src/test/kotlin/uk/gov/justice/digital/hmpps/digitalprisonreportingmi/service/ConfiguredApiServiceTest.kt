package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service

import jakarta.validation.ValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.ProductDefinitionRepository

class ConfiguredApiServiceTest {
  private val productDefinitionRepository: ProductDefinitionRepository = ProductDefinitionRepository()
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

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows when both range and non range filters are provided`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val filtersExcludingRange = mapOf("direction" to "in")
    val rangeFilters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataSet.first()

    whenever(configuredApiRepository.executeQuery(dataSet.query, rangeFilters, filtersExcludingRange, selectedPage, pageSize, sortColumn, sortedAsc)).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc)

    verify(configuredApiRepository, times(1)).executeQuery(dataSet.query, rangeFilters, filtersExcludingRange, selectedPage, pageSize, sortColumn, sortedAsc)
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows when both range and non range filters are provided`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val filtersExcludingRange = mapOf("direction" to "in")
    val rangeFilters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataSet.first()

    whenever(configuredApiRepository.count(rangeFilters, filtersExcludingRange, dataSet.query)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters)

    verify(configuredApiRepository, times(1)).count(rangeFilters, filtersExcludingRange, dataSet.query)
    assertEquals(Count(4), actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows when only range filters are provided`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val rangeFilters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataSet.first()

    whenever(configuredApiRepository.executeQuery(dataSet.query, rangeFilters, emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc)).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc)

    verify(configuredApiRepository, times(1)).executeQuery(dataSet.query, rangeFilters, emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc)
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows when only range filters are provided`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val rangeFilters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataSet.first()

    whenever(configuredApiRepository.count(rangeFilters, emptyMap(), dataSet.query)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters)

    verify(configuredApiRepository, times(1)).count(rangeFilters, emptyMap(), dataSet.query)
    assertEquals(Count(4), actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows when only non range filters are provided`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filtersExcludingRange = mapOf("direction" to "in")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataSet.first()

    whenever(configuredApiRepository.executeQuery(dataSet.query, emptyMap(), filtersExcludingRange, selectedPage, pageSize, sortColumn, sortedAsc)).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filtersExcludingRange, selectedPage, pageSize, sortColumn, sortedAsc)

    verify(configuredApiRepository, times(1)).executeQuery(dataSet.query, emptyMap(), filtersExcludingRange, selectedPage, pageSize, sortColumn, sortedAsc)
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows when only non range filters are provided`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "in")
    val filtersExcludingRange = mapOf("direction" to "in")
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataSet.first()

    whenever(configuredApiRepository.count(emptyMap(), filtersExcludingRange, dataSet.query)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters)

    verify(configuredApiRepository, times(1)).count(emptyMap(), filtersExcludingRange, dataSet.query)
    assertEquals(Count(4), actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a list of rows regardless of the casing of the values of the non range filters`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "In", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val filtersExcludingRange = mapOf("direction" to "In")
    val rangeFilters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataSet.first()

    whenever(configuredApiRepository.executeQuery(dataSet.query, rangeFilters, filtersExcludingRange, selectedPage, pageSize, sortColumn, sortedAsc)).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc)

    verify(configuredApiRepository, times(1)).executeQuery(dataSet.query, rangeFilters, filtersExcludingRange, selectedPage, pageSize, sortColumn, sortedAsc)
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows regardless of the casing of the values of the non range filters`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "In", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val filtersExcludingRange = mapOf("direction" to "In")
    val rangeFilters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataSet.first()

    whenever(configuredApiRepository.count(rangeFilters, filtersExcludingRange, dataSet.query)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters)

    verify(configuredApiRepository, times(1)).count(rangeFilters, filtersExcludingRange, dataSet.query)
    assertEquals(Count(4), actual)
  }

  @Test
  fun `the service calls the repository without filters if no filters are provided`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataSet.first()
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    whenever(configuredApiRepository.executeQuery(dataSet.query, emptyMap(), emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc)).thenReturn(
      listOf(
        mapOf("PRISONNUMBER" to "1"),
      ),
    )

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc)

    verify(configuredApiRepository, times(1)).executeQuery(dataSet.query, emptyMap(), emptyMap(), 1, 10, "date", true)
    assertEquals(
      listOf(
        mapOf("prisonNumber" to "1"),
      ),
      actual,
    )
  }

  @Test
  fun `the service count method calls the repository without filters if no filters are provided`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataSet.first()

    whenever(configuredApiRepository.count(emptyMap(), emptyMap(), dataSet.query)).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, emptyMap())

    verify(configuredApiRepository, times(1)).count(emptyMap(), emptyMap(), dataSet.query)
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
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc)
    }
    assertEquals("${ConfiguredApiService.INVALID_REPORT_ID_MESSAGE} $reportId", e.message)
    verify(configuredApiRepository, times(0)).executeQuery(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `validateAndCount should throw an exception for invalid report id`() {
    val reportId = "random report id"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters)
    }
    assertEquals("${ConfiguredApiService.INVALID_REPORT_ID_MESSAGE} $reportId", e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception for invalid report variant`() {
    val reportId = "external-movements"
    val reportVariantId = "non existent variant"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc)
    }
    assertEquals("${ConfiguredApiService.INVALID_REPORT_VARIANT_ID_MESSAGE} $reportVariantId", e.message)
    verify(configuredApiRepository, times(0)).executeQuery(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `validateAndCount should throw an exception for invalid report variant`() {
    val reportId = "external-movements"
    val reportVariantId = "non existent variant"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters)
    }
    assertEquals("${ConfiguredApiService.INVALID_REPORT_VARIANT_ID_MESSAGE} $reportVariantId", e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception for invalid sort column`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "abc"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc)
    }
    assertEquals("Invalid sortColumn provided: abc", e.message)
    verify(configuredApiRepository, times(0)).executeQuery(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception for invalid filter`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("non existent filter" to "blah")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc)
    }
    assertEquals(ConfiguredApiService.INVALID_FILTERS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).executeQuery(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `validateAndCount should throw an exception for invalid filter`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("non existent filter" to "blah")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters)
    }
    assertEquals(ConfiguredApiService.INVALID_FILTERS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception when having a valid and an invalid filter`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("non existent filter" to "blah", "date$RANGE_FILTER_START_SUFFIX" to "2023-01-01")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc)
    }
    assertEquals(ConfiguredApiService.INVALID_FILTERS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).executeQuery(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `validateAndCount should throw an exception when having a valid and an invalid filter`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("non existent filter" to "blah", "date$RANGE_FILTER_START_SUFFIX" to "2023-01-01")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters)
    }
    assertEquals(ConfiguredApiService.INVALID_FILTERS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception when having invalid static options for a filter and a valid range filter`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "randomValue", "date$RANGE_FILTER_START_SUFFIX" to "2023-01-01")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc)
    }
    assertEquals(ConfiguredApiService.INVALID_STATIC_OPTIONS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).executeQuery(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `validateAndCount should throw an exception when having invalid static options for a filter and a valid range filter`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "randomValue", "date$RANGE_FILTER_START_SUFFIX" to "2023-01-01")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters)
    }
    assertEquals(ConfiguredApiService.INVALID_STATIC_OPTIONS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception when having invalid static options for a filter and no range filters`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "randomValue")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc)
    }
    assertEquals(ConfiguredApiService.INVALID_STATIC_OPTIONS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).executeQuery(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `validateAndCount should throw an exception when having invalid static options for a filter and no range filters`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "randomValue")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters)
    }
    assertEquals(ConfiguredApiService.INVALID_STATIC_OPTIONS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any())
  }

  @Test
  fun `validateAndFetchData should throw an exception when having an invalid range filter`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "abc")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc)
    }
    assertEquals("Invalid value abc for filter date. Cannot be parsed as a date.", e.message)
    verify(configuredApiRepository, times(0)).executeQuery(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `validateAndCount should throw an exception when having an invalid range filter`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "abc")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters)
    }
    assertEquals("Invalid value abc for filter date. Cannot be parsed as a date.", e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any())
  }

  @Test
  fun `should call the configuredApiRepository with the default sort column if none is provided`() {
    val reportId = "external-movements"
    val reportVariantId = "last-month"
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val filtersExcludingRange = mapOf("direction" to "in")
    val rangeFilters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataSet.first()

    whenever(configuredApiRepository.executeQuery(dataSet.query, rangeFilters, filtersExcludingRange, selectedPage, pageSize, sortColumn, sortedAsc)).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, null, sortedAsc)

    verify(configuredApiRepository, times(1)).executeQuery(dataSet.query, rangeFilters, filtersExcludingRange, selectedPage, pageSize, sortColumn, sortedAsc)
    assertEquals(expectedServiceResult, actual)
  }
}
