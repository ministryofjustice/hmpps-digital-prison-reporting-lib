package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.jdbc.UncategorizedSQLException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.Filter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.DatasetHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IsoLocalDateTimeTypeAdaptor
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.JsonFileProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RedshiftDataApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.EXTERNAL_MOVEMENTS_PRODUCT_ID
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.FilterType.BOOLEAN
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.FilterType.DATE_RANGE_END
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.FilterType.DATE_RANGE_START
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.FilterType.DYNAMIC
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.FilterType.STANDARD
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Template
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Visible
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Effect
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_PERMIT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Rule
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Prompt
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.UUID

class ConfiguredApiServiceTest {
  private val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
    listOf("productDefinition.json"),
    DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
  )
  private val configuredApiRepository: ConfiguredApiRepository = mock<ConfiguredApiRepository>()
  private val redshiftDataApiRepository: RedshiftDataApiRepository = mock<RedshiftDataApiRepository>()
  private val athenaApiRepository: AthenaApiRepository = mock<AthenaApiRepository>()
  private val expectedRepositoryResult = listOf(
    mapOf(
      "PRISONNUMBER" to "1",
      "NAME" to "FirstName",
      "DATE" to "2023-05-20",
      "ORIGIN" to "OriginLocation",
      "DESTINATION" to "DestinationLocation",
      "DIRECTION" to "in",
      "TYPE" to "trn",
      "REASON" to "normal transfer",
    ),
  )
  private val expectedServiceResult = listOf(
    mapOf(
      "prisonNumber" to "1",
      "name" to "FirstName",
      "date" to "2023-05-20",
      "origin" to "OriginLocation",
      "destination" to "DestinationLocation",
      "direction" to "in",
      "type" to "trn",
      "reason" to "normal transfer",
    ),
  )
  private val authToken = mock<DprAuthAwareAuthenticationToken>()
  private val reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID
  private val reportVariantId = "last-month"
  private val policyEngineResult = "(origin_code='WWI' AND lower(direction)='out') OR (destination_code='WWI' AND lower(direction)='in')"
  private val tableIdGenerator: TableIdGenerator = TableIdGenerator()
  private val datasetHelper: DatasetHelper = DatasetHelper()
  private val configuredApiService = ConfiguredApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, datasetHelper)

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
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)
    val dataSourceName = singleReportProductDefinition.datasource.name

    whenever(
      configuredApiRepository.executeQuery(
        query = dataSet.query,
        filters = repositoryFilters,
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      query = dataSet.query,
      filters = repositoryFilters,
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      reportId = reportId,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
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
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.executeQuery(
        query = dataSet.query,
        filters = repositoryFilters,
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dynamicFilterFieldId = setOf(reportFieldId),
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
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
      setOf(reportFieldId),
      prefix,
    )

    verify(configuredApiRepository, times(1)).executeQuery(
      query = dataSet.query,
      filters = repositoryFilters,
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      reportId = reportId,
      policyEngineResult = policyEngineResult,
      dynamicFilterFieldId = setOf(reportFieldId),
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the correct dataset query, a permit policy and get a list of rows when a datasetForFilter is provided`() {
    val selectedPage = 1L
    val pageSize = 30L
    val sortedAsc = true
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val estNameSchemaFieldName = "establishment_name"
    val estCodeSchemaFieldName = "establishment_code"
    val filterDataset = Dataset(
      "establishment-dataset",
      "establishment-dataset-name",
      "select * from table",
      Schema(
        listOf(
          SchemaField(estCodeSchemaFieldName, ParameterType.String, "Establishment Code"),
          SchemaField(estNameSchemaFieldName, ParameterType.String, "Establishment Name"),
        ),
      ),
    )
    val expectedEstablishmentRepositoryResult = listOf(
      mapOf(
        estNameSchemaFieldName.uppercase() to "establishment name",
        estCodeSchemaFieldName.uppercase() to "establishment code",
      ),
    )
    val expectedServiceResult = listOf(
      mapOf(
        estNameSchemaFieldName to "establishment name",
        estCodeSchemaFieldName to "establishment code",
      ),
    )
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.executeQuery(
        query = filterDataset.query,
        filters = emptyList(),
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = estNameSchemaFieldName,
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = POLICY_PERMIT,
        dynamicFilterFieldId = linkedSetOf(estNameSchemaFieldName, estCodeSchemaFieldName),
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(expectedEstablishmentRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(
      reportId = reportId,
      reportVariantId = reportVariantId,
      filters = emptyMap(),
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = estNameSchemaFieldName,
      sortedAsc = sortedAsc,
      userToken = authToken,
      reportFieldId = linkedSetOf(estNameSchemaFieldName, estCodeSchemaFieldName),
      datasetForFilter = filterDataset,
    )

    verify(configuredApiRepository, times(1)).executeQuery(
      query = filterDataset.query,
      filters = emptyList(),
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = estNameSchemaFieldName,
      sortedAsc = sortedAsc,
      reportId = reportId,
      policyEngineResult = POLICY_PERMIT,
      dynamicFilterFieldId = linkedSetOf(estNameSchemaFieldName, estCodeSchemaFieldName),
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows when both range and non range filters are provided`() {
    val filters = mapOf("direction" to "in", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("direction", "in"), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.count(
        filters = repositoryFilters,
        query = dataSet.query,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)

    verify(configuredApiRepository, times(1)).count(
      filters = repositoryFilters,
      query = dataSet.query,
      reportId = reportId,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
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
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.executeQuery(
        query = dataSet.query,
        filters = repositoryFilters,
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      query = dataSet.query,
      filters = repositoryFilters,
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      reportId = reportId,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows when only range filters are provided`() {
    val filters = mapOf("date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.count(
        filters = repositoryFilters,
        query = dataSet.query,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)

    verify(configuredApiRepository, times(1)).count(
      filters = repositoryFilters,
      query = dataSet.query,
      reportId = reportId,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
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
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.executeQuery(
        query = dataSet.query,
        filters = repositoryFilters,
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
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
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows when only non range filters are provided`() {
    val filters = mapOf("direction" to "in")
    val repositoryFilters = listOf(Filter("direction", "in"))
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.count(
        filters = repositoryFilters,
        query = dataSet.query,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)

    verify(configuredApiRepository, times(1)).count(
      filters = repositoryFilters,
      query = dataSet.query,
      reportId = reportId,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
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
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.executeQuery(
        query = dataSet.query,
        filters = repositoryFilters,
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      query = dataSet.query,
      filters = repositoryFilters,
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      reportId = reportId,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `the policy engine should parse a null action correctly and return TRUE for an access policy with a matching condition`() {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionPolicyNoAction.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
    )
    val configuredApiService = ConfiguredApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, datasetHelper)
    whenever(authToken.authorities).thenReturn(listOf(SimpleGrantedAuthority("USER-ROLE-1")))
    val policyEngineResult = "TRUE"
    val reportId = "definition-policy-no-action"
    val filters = mapOf("direction" to "In", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("direction", "In"), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.executeQuery(
        query = dataSet.query,
        filters = repositoryFilters,
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      query = dataSet.query,
      filters = repositoryFilters,
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      reportId = reportId,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with the corresponding arguments and get a count of rows regardless of the casing of the values of the non range filters`() {
    val filters = mapOf("direction" to "In", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("direction", "In"), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.count(
        filters = repositoryFilters,
        query = dataSet.query,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)

    verify(configuredApiRepository, times(1)).count(
      filters = repositoryFilters,
      query = dataSet.query,
      reportId = reportId,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
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
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.executeQuery(
        query = dataSet.query,
        filters = emptyList(),
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(
      listOf(
        mapOf("PRISONNUMBER" to "1"),
      ),
    )

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      query = dataSet.query,
      filters = emptyList(),
      selectedPage = 1,
      pageSize = 10,
      sortColumn = "date",
      sortedAsc = true,
      reportId = reportId,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
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
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.count(
        filters = emptyList(),
        query = dataSet.query,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(4)

    val actual = configuredApiService.validateAndCount(reportId, reportVariantId, emptyMap(), authToken)

    verify(configuredApiRepository, times(1)).count(
      filters = emptyList(),
      query = dataSet.query,
      reportId = reportId,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
    )
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
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any(), any())
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
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any(), any())
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
      any(),
      any(),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["destination", "invalid field name"])
  fun `validateAndFetchData should throw an exception for invalid dynamic filter`(fieldId: String) {
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, reportVariantId, emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc, authToken, setOf(fieldId), "ab")
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
      configuredApiService.validateAndFetchData(reportId, reportVariantId, emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc, authToken, setOf(fieldId), "ab")
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
      any(),
    )
  }

  @Test
  fun `validateAndFetchData should throw an exception for a mandatory filter with no value`() {
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, "last-year", emptyMap(), selectedPage, pageSize, sortColumn, sortedAsc, authToken)
    }
    assertEquals(ConfiguredApiService.MISSING_MANDATORY_FILTER_MESSAGE + " Date", e.message)
  }

  @Test
  fun `validateAndFetchData should not throw an exception for a mandatory filter with no value if querying for a specific field`() {
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true

    val actual = configuredApiService.validateAndFetchData(
      reportId = reportId,
      reportVariantId = "last-year",
      filters = emptyMap(),
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      userToken = authToken,
      reportFieldId = setOf("name"),
    )

    assertThat(actual).isNotNull
  }

  @Test
  fun `validateAndFetchData should throw an exception for a filter value that does not match the validation pattern`() {
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val filters = mapOf(
      "date.start" to "2000-01-02",
      "origin" to "Invalid",
    )
    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndFetchData(reportId, "last-year", filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)
    }
    assertEquals(ConfiguredApiService.FILTER_VALUE_DOES_NOT_MATCH_PATTERN_MESSAGE + " Invalid [A-Z]{3,3}", e.message)
  }

  @Test
  fun `validateAndFetchData should not throw an exception for a filter value that does not match the validation pattern if querying for a specific field`() {
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val filters = mapOf(
      "date.start" to "2000-01-02",
      "origin" to "Invalid",
    )

    val actual = configuredApiService.validateAndFetchData(
      reportId = reportId,
      reportVariantId = "last-year",
      filters = filters,
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      userToken = authToken,
      reportFieldId = setOf("origin"),
    )

    assertThat(actual).isNotNull()
  }

  @Test
  fun `validateAndCount should throw an exception for a mandatory filter with no value`() {
    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, "last-year", emptyMap(), authToken)
    }
    assertEquals(ConfiguredApiService.MISSING_MANDATORY_FILTER_MESSAGE + " Date", e.message)
  }

  @Test
  fun `validateAndCount should throw an exception for a filter value that does not match the validation pattern`() {
    val filters = mapOf(
      "date.start" to "2000-01-02",
      "origin" to "Invalid",
    )
    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, "last-year", filters, authToken)
    }
    assertEquals(ConfiguredApiService.FILTER_VALUE_DOES_NOT_MATCH_PATTERN_MESSAGE + " Invalid [A-Z]{3,3}", e.message)
  }

  @Test
  fun `validateAndExecuteStatementAsync should throw an exception for a mandatory filter with no value`() {
    val sortColumn = "date"
    val sortedAsc = true

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndExecuteStatementAsync(reportId, "last-year", emptyMap(), sortColumn, sortedAsc, authToken)
    }
    assertEquals(ConfiguredApiService.MISSING_MANDATORY_FILTER_MESSAGE + " Date", e.message)
  }

  @Test
  fun `validateAndExecuteStatementAsync should throw an exception for a filter value that does not match the validation pattern`() {
    val sortColumn = "date"
    val sortedAsc = true
    val filters = mapOf(
      "date.start" to "2000-01-02",
      "origin" to "Invalid",
    )
    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndExecuteStatementAsync(reportId, "last-year", filters, sortColumn, sortedAsc, authToken)
    }
    assertEquals(ConfiguredApiService.FILTER_VALUE_DOES_NOT_MATCH_PATTERN_MESSAGE + " Invalid [A-Z]{3,3}", e.message)
  }

  @Test
  fun `validateAndCount should throw an exception for invalid filter`() {
    val filters = mapOf("non existent filter" to "blah")

    val e = org.junit.jupiter.api.assertThrows<ValidationException> {
      configuredApiService.validateAndCount(reportId, reportVariantId, filters, authToken)
    }
    assertEquals(ConfiguredApiService.INVALID_FILTERS_MESSAGE, e.message)
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any(), any())
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
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any(), any())
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
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any(), any())
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
        setOf("name"),
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
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any(), any())
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
    verify(configuredApiRepository, times(0)).count(any(), any(), any(), any(), any(), any())
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
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.executeQuery(
        query = dataSet.query,
        filters = repositoryFilters,
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, null, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      query = dataSet.query,
      filters = repositoryFilters,
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      reportId = reportId,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the configuredApiRepository with no sort column if none is provided and there is no default`() {
    val dataSet =
      Dataset("datasetId", "datasetname", "select *", Schema(listOf(SchemaField("9", ParameterType.String, display = ""))))
    val report = Report(
      id = "6",
      name = "7",
      created = LocalDateTime.MAX,
      version = "8",
      dataset = "\$ref:datasetId",
      render = RenderMethod.SVG,
      classification = "someClassification",
      specification = Specification(
        template = Template.List,
        field = listOf(
          ReportField(
            name = "\$ref:9",
            display = "Number 9",
            formula = "",
            visible = Visible.TRUE,
            sortable = true,
            defaultSort = false,
          ),
        ),
        section = null,
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
    val configuredApiService = ConfiguredApiService(productDefRepo, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, datasetHelper)
    val dataSourceName = "name"

    whenever(productDefRepo.getProductDefinitions())
      .thenReturn(listOf(productDefinition))

    val singleReportProductDefinition = SingleReportProductDefinition(
      id = "1",
      name = "2",
      metadata = MetaData(
        author = "3",
        owner = "4",
        version = "5",
      ),
      policy = listOf(policy),
      reportDataset = dataSet,
      report = report,
      datasource = Datasource("id", dataSourceName),
      allDatasets = listOf(dataSet),
    )
    whenever(productDefRepo.getSingleReportProductDefinition(reportId, reportVariantId))
      .thenReturn(
        singleReportProductDefinition,
      )

    val selectedPage = 1L
    val pageSize = 10L
    val sortedAsc = true

    whenever(
      configuredApiRepository.executeQuery(
        query = dataSet.query,
        filters = emptyList(),
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = null,
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = POLICY_PERMIT,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, emptyMap(), selectedPage, pageSize, null, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      query = dataSet.query,
      filters = emptyList(),
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = null,
      sortedAsc = sortedAsc,
      reportId = reportId,
      policyEngineResult = POLICY_PERMIT,
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should throw an exception when a filter is passed as a non boolean value when a boolean is expected`() {
    val exception = Assertions.assertThrows(ValidationException::class.java) {
      configuredApiService.validateAndFetchData(
        reportId,
        reportVariantId,
        mapOf("is_closed" to "in"),
        1L,
        10L,
        "date",
        true,
        authToken,
      )
    }
    verifyNoInteractions(configuredApiRepository)
    assertThat(exception).message().isEqualTo("Invalid value in for filter is_closed. Cannot be parsed as a boolean.")
  }

  @Test
  fun `should call the repository with Boolean Filter type for boolean filters`() {
    val filters = mapOf("is_closed" to "true", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("is_closed", "true", BOOLEAN), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val selectedPage = 1L
    val pageSize = 10L
    val sortColumn = "date"
    val sortedAsc = true
    val dataSet = productDefinitionRepository.getProductDefinitions().first().dataset.first()
    val dataSourceName = productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId).datasource.name
    val singleReportProductDefinition =
      productDefinitionRepository.getSingleReportProductDefinition(reportId, reportVariantId)

    whenever(
      configuredApiRepository.executeQuery(
        query = dataSet.query,
        filters = repositoryFilters,
        selectedPage = selectedPage,
        pageSize = pageSize,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        reportId = reportId,
        policyEngineResult = policyEngineResult,
        dataSourceName = dataSourceName,
        productDefinition = singleReportProductDefinition,
      ),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.validateAndFetchData(reportId, reportVariantId, filters, selectedPage, pageSize, sortColumn, sortedAsc, authToken)

    verify(configuredApiRepository, times(1)).executeQuery(
      query = dataSet.query,
      filters = repositoryFilters,
      selectedPage = selectedPage,
      pageSize = pageSize,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      reportId = reportId,
      policyEngineResult = policyEngineResult,
      dataSourceName = dataSourceName,
      productDefinition = singleReportProductDefinition,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should make the async call to the RedshiftDataApiRepository for datamart with all provided arguments when validateAndExecuteStatementAsync is called`() {
    val configuredApiService = ConfiguredApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, datasetHelper)
    val filters = mapOf("is_closed" to "true", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("is_closed", "true", BOOLEAN), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val sortColumn = "date"
    val sortedAsc = true
    val productDefinition = productDefinitionRepository.getProductDefinitions().first()
    val singleReportProductDefinition = productDefinitionRepository.getSingleReportProductDefinition(productDefinition.id, productDefinition.report.first().id)
    val executionId = UUID.randomUUID().toString()
    val tableId = executionId.replace("-", "_")
    val statementExecutionResponse = StatementExecutionResponse(tableId, executionId)
    whenever(
      redshiftDataApiRepository.executeQueryAsync(
        productDefinition = singleReportProductDefinition,
        filters = repositoryFilters,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        policyEngineResult = policyEngineResult,
        prompts = emptyList(),
        userToken = authToken,
      ),
    ).thenReturn(statementExecutionResponse)

    val actual = configuredApiService.validateAndExecuteStatementAsync(reportId, reportVariantId, filters, sortColumn, sortedAsc, authToken)

    verify(redshiftDataApiRepository, times(1)).executeQueryAsync(
      productDefinition = singleReportProductDefinition,
      filters = repositoryFilters,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      policyEngineResult = policyEngineResult,
      prompts = emptyList(),
      userToken = authToken,
    )
    assertEquals(statementExecutionResponse, actual)
  }

  @ParameterizedTest
  @CsvSource(
    "nomis_db, nomis, productDefinitionNomis.json",
    "bodmis_db, bodmis, productDefinitionBodmis.json",
  )
  fun `should make the async call to the AthenaApiRepository for nomis and bodmis with all provided arguments when validateAndExecuteStatementAsync is called`(database: String, catalog: String, definitionFile: String) {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf(definitionFile),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
    )
    val configuredApiService = ConfiguredApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, datasetHelper)
    val filters = mapOf("is_closed" to "true", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("is_closed", "true", BOOLEAN), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val sortColumn = "date"
    val sortedAsc = true
    val productDefinition = productDefinitionRepository.getProductDefinitions().first()
    val singleReportProductDefinition = productDefinitionRepository.getSingleReportProductDefinition(productDefinition.id, productDefinition.report.first().id)
    val executionId = UUID.randomUUID().toString()
    val tableId = executionId.replace("-", "_")
    val statementExecutionResponse = StatementExecutionResponse(tableId, executionId)
    whenever(
      athenaApiRepository.executeQueryAsync(
        productDefinition = singleReportProductDefinition,
        filters = repositoryFilters,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        policyEngineResult = policyEngineResult,
        prompts = emptyList(),
        userToken = authToken,
      ),
    ).thenReturn(statementExecutionResponse)

    val actual = configuredApiService.validateAndExecuteStatementAsync(reportId, reportVariantId, filters, sortColumn, sortedAsc, authToken)

    verify(athenaApiRepository, times(1)).executeQueryAsync(
      productDefinition = singleReportProductDefinition,
      filters = repositoryFilters,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      policyEngineResult = policyEngineResult,
      prompts = emptyList(),
      userToken = authToken,
    )
    verifyNoInteractions(redshiftDataApiRepository)
    assertEquals(statementExecutionResponse, actual)
  }

  @Test
  fun `should call the RedshiftDataApiRepository for datamart with the statement execution ID when getStatementStatus is called`() {
    val configuredApiService = ConfiguredApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, datasetHelper)
    val statementId = "statementId"
    val status = "FINISHED"
    val duration = 278109264L
    val resultRows = 10L
    val resultSize = 100L
    val statementExecutionStatus = StatementExecutionStatus(
      status,
      duration,
      resultRows,
      resultSize,
    )
    whenever(
      redshiftDataApiRepository.getStatementStatus(statementId),
    ).thenReturn(statementExecutionStatus)

    val actual = configuredApiService.getStatementStatus(statementId, "external-movements", "last-month")
    verify(redshiftDataApiRepository, times(1)).getStatementStatus(statementId)
    verifyNoInteractions(athenaApiRepository)
    assertEquals(statementExecutionStatus, actual)
  }

  @ParameterizedTest
  @ValueSource(strings = ["productDefinitionNomis.json", "productDefinitionBodmis.json"])
  fun `should call the AthenaApiRepository for nomis and bodmis with the statement execution ID when getStatementStatus is called`(definitionFile: String) {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf(definitionFile),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
    )
    val configuredApiService = ConfiguredApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, datasetHelper)
    val statementId = "statementId"
    val status = "FINISHED"
    val duration = 278109264L
    val resultRows = 10L
    val resultSize = 100L
    val statementExecutionStatus = StatementExecutionStatus(
      status,
      duration,
      resultRows,
      resultSize,
    )
    whenever(
      athenaApiRepository.getStatementStatus(statementId),
    ).thenReturn(statementExecutionStatus)

    val actual = configuredApiService.getStatementStatus(statementId, "external-movements", "last-month")
    verify(athenaApiRepository, times(1)).getStatementStatus(statementId)
    verifyNoInteractions(redshiftDataApiRepository)
    assertEquals(statementExecutionStatus, actual)
  }

  @Test
  fun `should call the RedshiftDataApiRepository for datamart with the statement execution ID when cancelStatementExecution is called`() {
    val configuredApiService = ConfiguredApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, datasetHelper)
    val statementId = "statementId"
    val statementCancellationResponse = StatementCancellationResponse(
      true,
    )
    whenever(
      redshiftDataApiRepository.cancelStatementExecution(statementId),
    ).thenReturn(statementCancellationResponse)

    val actual = configuredApiService.cancelStatementExecution(statementId, "external-movements", "last-month")
    verify(redshiftDataApiRepository, times(1)).cancelStatementExecution(statementId)
    verifyNoInteractions(athenaApiRepository)
    assertEquals(statementCancellationResponse, actual)
  }

  @ParameterizedTest
  @ValueSource(strings = ["productDefinitionNomis.json", "productDefinitionBodmis.json"])
  fun `should call the AthenaApiRepository for nomis and bodmis with the statement execution ID when cancelStatementExecution is called`(definitionFile: String) {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf(definitionFile),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
    )
    val configuredApiService = ConfiguredApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, datasetHelper)
    val statementId = "statementId"
    val statementCancellationResponse = StatementCancellationResponse(true)
    whenever(
      athenaApiRepository.cancelStatementExecution(statementId),
    ).thenReturn(statementCancellationResponse)

    val actual = configuredApiService.cancelStatementExecution(statementId, "external-movements", "last-month")
    verify(athenaApiRepository, times(1)).cancelStatementExecution(statementId)
    verifyNoInteractions(redshiftDataApiRepository)
    assertEquals(statementCancellationResponse, actual)
  }

  @Test
  fun `validateAndExecuteStatementAsync should not fail validation for filters which were converted from DPD parameters and convert these filters to prompts`() {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionWithParameters.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
    )
    val productDefinition = productDefinitionRepository.getProductDefinitions().first()
    val singleReportProductDefinition = productDefinitionRepository.getSingleReportProductDefinition(productDefinition.id, productDefinition.report.first().id)
    val configuredApiService = ConfiguredApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, datasetHelper)
    val parameterName = "prisoner_number"
    val parameterValue = "somePrisonerNumber"
    val filters = mapOf(
      "origin" to "someOrigin",
      parameterName to parameterValue,
    )
    val repositoryFilters = listOf(Filter("origin", "someOrigin", STANDARD))
    val prompts = listOf(Prompt(parameterName, parameterValue, FilterType.Text))
    val sortColumn = "date"
    val sortedAsc = true
    val executionId = UUID.randomUUID().toString()
    val tableId = executionId.replace("-", "_")
    val statementExecutionResponse = StatementExecutionResponse(tableId, executionId)
    whenever(
      redshiftDataApiRepository.executeQueryAsync(
        productDefinition = singleReportProductDefinition,
        filters = repositoryFilters,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        policyEngineResult = policyEngineResult,
        prompts = prompts,
        userToken = authToken,
      ),
    ).thenReturn(statementExecutionResponse)

    val actual = configuredApiService.validateAndExecuteStatementAsync("external-movements-with-parameters", reportVariantId, filters, sortColumn, sortedAsc, authToken)

    verify(redshiftDataApiRepository, times(1)).executeQueryAsync(
      productDefinition = singleReportProductDefinition,
      filters = repositoryFilters,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      policyEngineResult = policyEngineResult,
      prompts = prompts,
      userToken = authToken,
    )
    assertEquals(statementExecutionResponse, actual)
  }

  @Test
  fun `should call the repository with all provided arguments when getStatementResult is called`() {
    val tableId = TableIdGenerator().generateNewExternalTableId()
    val selectedPage = 1L
    val pageSize = 20L
    whenever(
      redshiftDataApiRepository.getPaginatedExternalTableResult(tableId, selectedPage, pageSize),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.getStatementResult(
      tableId,
      reportId,
      reportVariantId,
      selectedPage = selectedPage,
      pageSize = pageSize,
    )

    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `getStatementResult should apply formulas to the rows returned by the repository`() {
    val selectedPage = 1L
    val pageSize = 20L
    val expectedRepositoryResult = listOf(
      mapOf("PRISONNUMBER" to "1", "NAME" to "FirstName", "ORIGIN" to "OriginLocation", "ORIGIN_CODE" to "abc"),
    )
    val expectedServiceResult = listOf(
      mapOf("prisonNumber" to "1", "name" to "FirstName", "origin" to "OriginLocation", "origin_code" to "OriginLocation"),
    )
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionWithFormula.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
    )
    val configuredApiService = ConfiguredApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, datasetHelper)
    val executionID = UUID.randomUUID().toString()
    whenever(
      redshiftDataApiRepository.getPaginatedExternalTableResult(executionID, selectedPage, pageSize),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.getStatementResult(
      tableId = executionID,
      reportId = reportId,
      reportVariantId = reportVariantId,
      selectedPage = selectedPage,
      pageSize = pageSize,
    )

    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with all provided arguments when getSummaryResult is called`() {
    val tableId = TableIdGenerator().generateNewExternalTableId()
    val summaryId = "summaryId"
    whenever(
      redshiftDataApiRepository.getFullExternalTableResult(tableIdGenerator.getTableSummaryId(tableId, summaryId)),
    ).thenReturn(listOf(mapOf("TOTAL" to 1)))

    val actual = configuredApiService.getSummaryResult(
      tableId,
      summaryId,
      reportId,
      reportVariantId,
    )

    assertEquals(listOf(mapOf("total" to 1)), actual)
  }

  @Test
  fun `should create and query summary table when it doesn't exist`() {
    val tableId = TableIdGenerator().generateNewExternalTableId()
    val summaryId = "summaryId"
    whenever(
      redshiftDataApiRepository.getFullExternalTableResult(tableIdGenerator.getTableSummaryId(tableId, summaryId)),
    )
      .thenThrow(UncategorizedSQLException("EntityNotFoundException from glue - Entity Not Found", "", SQLException()))
      .thenReturn(listOf(mapOf("TOTAL" to 1)))

    val actual = configuredApiService.getSummaryResult(
      tableId,
      summaryId,
      reportId,
      reportVariantId,
    )

    assertEquals(listOf(mapOf("total" to 1)), actual)
    verify(redshiftDataApiRepository, times(2)).getFullExternalTableResult(any(), anyOrNull())
    verify(configuredApiRepository).createSummaryTable(any(), any(), any(), any())
  }

  @Test
  fun `should call the repository with all provided arguments when count is called`() {
    val tableId = "123"
    val expectedRepositoryResult = 5L
    whenever(
      redshiftDataApiRepository.count(tableId),
    ).thenReturn(expectedRepositoryResult)

    val actual = configuredApiService.count(tableId)

    assertEquals(Count(expectedRepositoryResult), actual)
  }
}
