package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.jdbc.UncategorizedSQLException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Count
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MetricData
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.Filter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IsoLocalDateTimeTypeAdaptor
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.JsonFileProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QUERY_FINISHED
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QUERY_STARTED
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RedshiftDataApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.EXTERNAL_MOVEMENTS_PRODUCT_ID
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.FilterType.BOOLEAN
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.FilterType.DATE_RANGE_END
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.FilterType.DATE_RANGE_START
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.FilterType.STANDARD
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dashboard
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MetaData
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MultiphaseQuery
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Parameter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReferenceType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportFilter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Schema
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleDashboardProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Specification
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Effect
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Rule
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.MissingTableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Prompt
import java.sql.SQLException
import java.util.UUID

class AsyncDataApiServiceTest {
  private val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
    listOf("productDefinition.json"),
    DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
    identifiedHelper = IdentifiedHelper(),
  )
  private val datamartProductDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
    listOf("productDefinitionDatamart.json"),
    DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
    identifiedHelper = IdentifiedHelper(),
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
  private val policyEngineResultTrue = "TRUE AND $policyEngineResult"
  private val tableIdGenerator: TableIdGenerator = TableIdGenerator()
  private val identifiedHelper: IdentifiedHelper = IdentifiedHelper()
  private val productDefinitionTokenPolicyChecker = mock<ProductDefinitionTokenPolicyChecker>()
  private val configuredApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)

  @BeforeEach
  fun setup() {
    whenever(authToken.getActiveCaseLoadId()).thenReturn("WWI")
    whenever(authToken.getCaseLoadIds()).thenReturn(listOf("WWI"))
  }

  @Test
  fun `should make the async call to the RedshiftDataApiRepository for datamart with all provided arguments when validateAndExecuteStatementAsync is called`() {
    val asyncDataApiService = AsyncDataApiService(datamartProductDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val filters = mapOf("is_closed" to "true", "date$RANGE_FILTER_START_SUFFIX" to "2023-04-25", "date$RANGE_FILTER_END_SUFFIX" to "2023-09-10")
    val repositoryFilters = listOf(Filter("is_closed", "true", BOOLEAN), Filter("date", "2023-04-25", DATE_RANGE_START), Filter("date", "2023-09-10", DATE_RANGE_END))
    val sortColumn = "date"
    val sortedAsc = true
    val productDefinition = datamartProductDefinitionRepository.getProductDefinitions().first()
    val singleReportProductDefinition = datamartProductDefinitionRepository.getSingleReportProductDefinition(productDefinition.id, productDefinition.report.first().id)
    val executionId = UUID.randomUUID().toString()
    val tableId = executionId.replace("-", "_")
    val statementExecutionResponse = StatementExecutionResponse(tableId, executionId)
    whenever(
      redshiftDataApiRepository.executeQueryAsync(
        filters = repositoryFilters,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        policyEngineResult = policyEngineResultTrue,
        prompts = emptyList(),
        userToken = authToken,
        query = singleReportProductDefinition.reportDataset.query,
        reportFilter = singleReportProductDefinition.report.filter,
        datasource = singleReportProductDefinition.datasource,
        reportSummaries = singleReportProductDefinition.report.summary,
        allDatasets = singleReportProductDefinition.allDatasets,
        productDefinitionId = singleReportProductDefinition.id,
        productDefinitionName = singleReportProductDefinition.name,
        reportOrDashboardId = singleReportProductDefinition.report.id,
        reportOrDashboardName = singleReportProductDefinition.report.name,
      ),
    ).thenReturn(statementExecutionResponse)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.validateAndExecuteStatementAsync(reportId, reportVariantId, filters, sortColumn, sortedAsc, authToken)

    verify(redshiftDataApiRepository, times(1)).executeQueryAsync(
      filters = repositoryFilters,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      policyEngineResult = policyEngineResultTrue,
      prompts = emptyList(),
      userToken = authToken,
      query = singleReportProductDefinition.reportDataset.query,
      reportFilter = singleReportProductDefinition.report.filter,
      datasource = singleReportProductDefinition.datasource,
      reportSummaries = singleReportProductDefinition.report.summary,
      allDatasets = singleReportProductDefinition.allDatasets,
      productDefinitionId = singleReportProductDefinition.id,
      productDefinitionName = singleReportProductDefinition.name,
      reportOrDashboardId = singleReportProductDefinition.report.id,
      reportOrDashboardName = singleReportProductDefinition.report.name,
    )
    assertEquals(statementExecutionResponse, actual)
  }

  @Test
  fun `should make the dashboard async call to the RedshiftDataApiRepository with all provided arguments when validateAndExecuteStatementAsync is called`() {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionWithDashboardDatamart.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val productDefinition = productDefinitionRepository.getProductDefinitions().first()
    val singleDashboardProductDefinition = productDefinitionRepository.getSingleDashboardProductDefinition(productDefinition.id, productDefinition.dashboard!!.first().id)
    val executionId = UUID.randomUUID().toString()
    val tableId = executionId.replace("-", "_")
    val statementExecutionResponse = StatementExecutionResponse(tableId, executionId)
    val caseload = "caseloadA"
    whenever(authToken.getActiveCaseLoadId()).thenReturn(caseload)
    whenever(authToken.getCaseLoadIds()).thenReturn(listOf(caseload))
    whenever(authToken.getRoles()).thenReturn(listOf("ROLE_PRISONS_REPORTING_USER"))
    val policyEngineResult = "(establishment_id='$caseload')"
    whenever(
      redshiftDataApiRepository.executeQueryAsync(
        filters = emptyList(),
        sortedAsc = true,
        policyEngineResult = policyEngineResult,
        prompts = emptyList(),
        userToken = authToken,
        query = singleDashboardProductDefinition.dashboardDataset.query,
        reportFilter = singleDashboardProductDefinition.dashboard.filter,
        datasource = singleDashboardProductDefinition.datasource,
        allDatasets = singleDashboardProductDefinition.allDatasets,
        productDefinitionId = singleDashboardProductDefinition.id,
        productDefinitionName = singleDashboardProductDefinition.name,
        reportOrDashboardId = singleDashboardProductDefinition.dashboard.id,
        reportOrDashboardName = singleDashboardProductDefinition.dashboard.name,
      ),
    ).thenReturn(statementExecutionResponse)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.validateAndExecuteStatementAsync(
      reportId = "missing-ethnicity-metrics",
      dashboardId = "age-breakdown-dashboard-1",
      userToken = authToken,
      filters = emptyMap(),
    )

    verify(redshiftDataApiRepository, times(1)).executeQueryAsync(
      filters = emptyList(),
      sortedAsc = true,
      policyEngineResult = policyEngineResult,
      prompts = emptyList(),
      userToken = authToken,
      query = singleDashboardProductDefinition.dashboardDataset.query,
      reportFilter = singleDashboardProductDefinition.dashboard.filter,
      datasource = singleDashboardProductDefinition.datasource,
      allDatasets = singleDashboardProductDefinition.allDatasets,
      productDefinitionId = singleDashboardProductDefinition.id,
      productDefinitionName = singleDashboardProductDefinition.name,
      reportOrDashboardId = singleDashboardProductDefinition.dashboard.id,
      reportOrDashboardName = singleDashboardProductDefinition.dashboard.name,
    )
    assertEquals(statementExecutionResponse, actual)
  }

  @Test
  fun `should call the RedshiftDataApiRepository for datamart with the statement execution ID when getStatementStatus is called`() {
    val asyncDataApiService = AsyncDataApiService(datamartProductDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
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

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.getStatementStatus(statementId, "external-movements", "last-month", authToken)
    verify(redshiftDataApiRepository, times(1)).getStatementStatus(statementId)
    verifyNoInteractions(athenaApiRepository)
    assertEquals(statementExecutionStatus, actual)
  }

  @Test
  fun `should call the RedshiftDataApiRepository for datamart with the statement execution ID when getDashboardStatementStatus is called`() {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionWithDashboardDatamart.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
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

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.getDashboardStatementStatus(statementId, "missing-ethnicity-metrics", "age-breakdown-dashboard-1", authToken)
    verify(redshiftDataApiRepository, times(1)).getStatementStatus(statementId)
    verifyNoInteractions(athenaApiRepository)
    assertEquals(statementExecutionStatus, actual)
  }

  @Test
  fun `should call the RedshiftDataApiRepository for datamart with the statement execution ID when report cancelStatementExecution is called`() {
    val asyncDataApiService = AsyncDataApiService(datamartProductDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val statementId = "statementId"
    val statementCancellationResponse = StatementCancellationResponse(
      true,
    )
    whenever(
      redshiftDataApiRepository.cancelStatementExecution(statementId),
    ).thenReturn(statementCancellationResponse)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.cancelStatementExecution(statementId, "external-movements", "last-month", authToken)
    verify(redshiftDataApiRepository, times(1)).cancelStatementExecution(statementId)
    verifyNoInteractions(athenaApiRepository)
    assertEquals(statementCancellationResponse, actual)
  }

  @Test
  fun `should call the RedshiftDataApiRepository for datamart with the statement execution ID when cancelDashboardStatementExecution is called`() {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionWithDashboardDatamart.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val statementId = "statementId"
    val statementCancellationResponse = StatementCancellationResponse(
      true,
    )
    whenever(
      redshiftDataApiRepository.cancelStatementExecution(statementId),
    ).thenReturn(statementCancellationResponse)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.cancelDashboardStatementExecution(statementId, "missing-ethnicity-metrics", "age-breakdown-dashboard-1", authToken)
    verify(redshiftDataApiRepository, times(1)).cancelStatementExecution(statementId)
    verifyNoInteractions(athenaApiRepository)
    assertEquals(statementCancellationResponse, actual)
  }

  @Test
  fun `validateAndExecuteStatementAsync should throw an exception for a mandatory filter with no value`() {
    val sortColumn = "date"
    val sortedAsc = true

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val e = assertThrows<ValidationException> {
      configuredApiService.validateAndExecuteStatementAsync(reportId, "last-year", emptyMap(), sortColumn, sortedAsc, authToken)
    }
    assertEquals(SyncDataApiService.MISSING_MANDATORY_FILTER_MESSAGE + " date", e.message)
  }

  @Test
  fun `validateAndExecuteStatementAsync should throw an exception for a filter value that does not match the validation pattern`() {
    val sortColumn = "date"
    val sortedAsc = true
    val filters = mapOf(
      "date.start" to "2000-01-02",
      "origin" to "Invalid",
    )

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val e = assertThrows<ValidationException> {
      configuredApiService.validateAndExecuteStatementAsync(reportId, "last-year", filters, sortColumn, sortedAsc, authToken)
    }
    assertEquals(SyncDataApiService.FILTER_VALUE_DOES_NOT_MATCH_PATTERN_MESSAGE + " Invalid [A-Z]{3,3}", e.message)
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
      identifiedHelper = IdentifiedHelper(),
    )
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
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
        filters = repositoryFilters,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        policyEngineResult = policyEngineResult,
        prompts = emptyList(),
        userToken = authToken,
        query = singleReportProductDefinition.reportDataset.query,
        reportFilter = singleReportProductDefinition.report.filter,
        datasource = singleReportProductDefinition.datasource,
        reportSummaries = singleReportProductDefinition.report.summary,
        allDatasets = singleReportProductDefinition.allDatasets,
        productDefinitionId = singleReportProductDefinition.id,
        productDefinitionName = singleReportProductDefinition.name,
        reportOrDashboardId = singleReportProductDefinition.report.id,
        reportOrDashboardName = singleReportProductDefinition.report.name,
      ),
    ).thenReturn(statementExecutionResponse)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.validateAndExecuteStatementAsync(reportId, reportVariantId, filters, sortColumn, sortedAsc, authToken)

    verify(athenaApiRepository, times(1)).executeQueryAsync(
      filters = repositoryFilters,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      policyEngineResult = policyEngineResult,
      prompts = emptyList(),
      userToken = authToken,
      query = singleReportProductDefinition.reportDataset.query,
      reportFilter = singleReportProductDefinition.report.filter,
      datasource = singleReportProductDefinition.datasource,
      reportSummaries = singleReportProductDefinition.report.summary,
      allDatasets = singleReportProductDefinition.allDatasets,
      productDefinitionId = singleReportProductDefinition.id,
      productDefinitionName = singleReportProductDefinition.name,
      reportOrDashboardId = singleReportProductDefinition.report.id,
      reportOrDashboardName = singleReportProductDefinition.report.name,
    )
    verifyNoInteractions(redshiftDataApiRepository)
    assertEquals(statementExecutionResponse, actual)
  }

  @Test
  fun `should make the dashboard async call to the AthenaDataApiRepository for nomis datasource with all provided arguments when validateAndExecuteStatementAsync is called`() {
    val reportId = "missing-ethnicity-metrics"
    val dashboardId = "test-dashboard-1"
    val productDefinitionRepository: ProductDefinitionRepository = mock<ProductDefinitionRepository>()
    val singleDashboardProductDefinition = mock<SingleDashboardProductDefinition>()
    val dashboard = mock<Dashboard>()
    val dashboardDataset = mock<Dataset>()
    val query = "select * from a"
    val schema = mock<Schema>()
    val field = mock<SchemaField>()
    val datasource = mock<Datasource>()
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val executionId = UUID.randomUUID().toString()
    val tableId = executionId.replace("-", "_")
    val statementExecutionResponse = StatementExecutionResponse(tableId, executionId)
    val caseload = "caseloadA"
    whenever(
      productDefinitionRepository.getSingleDashboardProductDefinition(
        definitionId = reportId,
        dashboardId = dashboardId,
      ),
    ).thenReturn(singleDashboardProductDefinition)
    whenever(singleDashboardProductDefinition.dashboard).thenReturn(dashboard)
    whenever(singleDashboardProductDefinition.id).thenReturn(dashboardId)
    whenever(singleDashboardProductDefinition.name).thenReturn("name")
    whenever(dashboard.id).thenReturn(dashboardId)
    whenever(dashboard.name).thenReturn("name2")
    whenever(dashboard.filter).thenReturn(mock<ReportFilter>())
    whenever(singleDashboardProductDefinition.dashboardDataset).thenReturn(dashboardDataset)
    whenever(dashboardDataset.query).thenReturn(query)
    whenever(dashboardDataset.multiphaseQuery).thenReturn(null)
    whenever(dashboardDataset.schema).thenReturn(schema)
    whenever(schema.field).thenReturn(listOf(field))
    whenever(field.name).thenReturn("fieldName")
    whenever(singleDashboardProductDefinition.allDatasets).thenReturn(listOf(dashboardDataset))
    whenever(singleDashboardProductDefinition.datasource).thenReturn(datasource)
    whenever(datasource.name).thenReturn("NOMIS")
    whenever(authToken.getActiveCaseLoadId()).thenReturn(caseload)
    whenever(authToken.getCaseLoadIds()).thenReturn(listOf(caseload))
    whenever(authToken.getRoles()).thenReturn(listOf("ROLE_PRISONS_REPORTING_USER"))
    val policyEngineResult = Policy.PolicyResult.POLICY_DENY
    whenever(
      athenaApiRepository.executeQueryAsync(
        filters = emptyList(),
        sortedAsc = true,
        policyEngineResult = policyEngineResult,
        prompts = emptyList(),
        userToken = authToken,
        query = singleDashboardProductDefinition.dashboardDataset.query,
        reportFilter = singleDashboardProductDefinition.dashboard.filter,
        datasource = singleDashboardProductDefinition.datasource,
        allDatasets = singleDashboardProductDefinition.allDatasets,
        productDefinitionId = singleDashboardProductDefinition.id,
        productDefinitionName = singleDashboardProductDefinition.name,
        reportOrDashboardId = singleDashboardProductDefinition.dashboard.id,
        reportOrDashboardName = singleDashboardProductDefinition.dashboard.name,
      ),
    ).thenReturn(statementExecutionResponse)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.validateAndExecuteStatementAsync(
      reportId = reportId,
      dashboardId = dashboardId,
      userToken = authToken,
      filters = emptyMap(),
    )

    verify(athenaApiRepository, times(1)).executeQueryAsync(
      filters = emptyList(),
      sortedAsc = true,
      policyEngineResult = policyEngineResult,
      prompts = emptyList(),
      userToken = authToken,
      query = singleDashboardProductDefinition.dashboardDataset.query,
      reportFilter = singleDashboardProductDefinition.dashboard.filter,
      datasource = singleDashboardProductDefinition.datasource,
      allDatasets = singleDashboardProductDefinition.allDatasets,
      productDefinitionId = singleDashboardProductDefinition.id,
      productDefinitionName = singleDashboardProductDefinition.name,
      reportOrDashboardId = singleDashboardProductDefinition.dashboard.id,
      reportOrDashboardName = singleDashboardProductDefinition.dashboard.name,
    )
    assertEquals(statementExecutionResponse, actual)
  }

  @Test
  fun `should deduplicate prompts when calling the AthenaApiRepository to execute a dashboard multiphase query`() {
    val reportId = "missing-ethnicity-metrics"
    val dashboardId = "test-dashboard-1"
    val productDefinitionRepository: ProductDefinitionRepository = mock<ProductDefinitionRepository>()
    val singleDashboardProductDefinition = mock<SingleDashboardProductDefinition>()
    val dashboard = mock<Dashboard>()
    val dashboardDataset = mock<Dataset>()
    val query = ""
    val schema = mock<Schema>()
    val field = mock<SchemaField>()
    val datasource = mock<Datasource>()
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val executionId = UUID.randomUUID().toString()
    val tableId = executionId.replace("-", "_")
    val statementExecutionResponse = StatementExecutionResponse(tableId, executionId)
    val parameterName = "establishment_code"
    val parameterValue = "BFI"
    val parameter = Parameter(0, parameterName, ParameterType.String, FilterType.AutoComplete, "Estabishment Code", true, ReferenceType.ESTABLISHMENT)
    val multiphaseQuery1 = MultiphaseQuery(0, datasource, "SELECT * FROM a", listOf(parameter))
    val multiphaseQuery2 = MultiphaseQuery(1, datasource, "SELECT * FROM b", listOf(parameter))
    val caseload = "caseloadA"
    val prompt = Prompt(parameterName, parameterValue, FilterType.AutoComplete)
    whenever(
      productDefinitionRepository.getSingleDashboardProductDefinition(
        definitionId = reportId,
        dashboardId = dashboardId,
      ),
    ).thenReturn(singleDashboardProductDefinition)
    whenever(singleDashboardProductDefinition.dashboard).thenReturn(dashboard)
    whenever(singleDashboardProductDefinition.id).thenReturn(dashboardId)
    whenever(singleDashboardProductDefinition.name).thenReturn("name")
    whenever(dashboard.id).thenReturn(dashboardId)
    whenever(dashboard.name).thenReturn("name2")
    whenever(dashboard.filter).thenReturn(mock<ReportFilter>())
    whenever(singleDashboardProductDefinition.dashboardDataset).thenReturn(dashboardDataset)
    whenever(dashboardDataset.query).thenReturn(query)
    whenever(dashboardDataset.multiphaseQuery).thenReturn(listOf(multiphaseQuery1, multiphaseQuery2))
    whenever(dashboardDataset.schema).thenReturn(schema)
    whenever(schema.field).thenReturn(listOf(field))
    whenever(field.name).thenReturn("fieldName")
    whenever(singleDashboardProductDefinition.allDatasets).thenReturn(listOf(dashboardDataset))
    whenever(singleDashboardProductDefinition.datasource).thenReturn(datasource)
    whenever(datasource.name).thenReturn("NOMIS")
    whenever(authToken.getActiveCaseLoadId()).thenReturn(caseload)
    whenever(authToken.getCaseLoadIds()).thenReturn(listOf(caseload))
    whenever(authToken.getRoles()).thenReturn(listOf("ROLE_PRISONS_REPORTING_USER"))
    val policyEngineResult = Policy.PolicyResult.POLICY_DENY
    whenever(
      athenaApiRepository.executeQueryAsync(
        filters = emptyList(),
        sortedAsc = true,
        policyEngineResult = policyEngineResult,
        prompts = listOf(prompt),
        userToken = authToken,
        query = singleDashboardProductDefinition.dashboardDataset.query,
        multiphaseQueries = listOf(multiphaseQuery1, multiphaseQuery2),
        reportFilter = singleDashboardProductDefinition.dashboard.filter,
        datasource = singleDashboardProductDefinition.datasource,
        allDatasets = singleDashboardProductDefinition.allDatasets,
        productDefinitionId = singleDashboardProductDefinition.id,
        productDefinitionName = singleDashboardProductDefinition.name,
        reportOrDashboardId = singleDashboardProductDefinition.dashboard.id,
        reportOrDashboardName = singleDashboardProductDefinition.dashboard.name,
      ),
    ).thenReturn(statementExecutionResponse)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.validateAndExecuteStatementAsync(
      reportId = reportId,
      dashboardId = dashboardId,
      userToken = authToken,
      filters = mapOf(parameterName to parameterValue),
    )

    verify(athenaApiRepository, times(1)).executeQueryAsync(
      filters = emptyList(),
      sortedAsc = true,
      policyEngineResult = policyEngineResult,
      prompts = listOf(prompt),
      userToken = authToken,
      query = singleDashboardProductDefinition.dashboardDataset.query,
      multiphaseQueries = listOf(multiphaseQuery1, multiphaseQuery2),
      reportFilter = singleDashboardProductDefinition.dashboard.filter,
      datasource = singleDashboardProductDefinition.datasource,
      allDatasets = singleDashboardProductDefinition.allDatasets,
      productDefinitionId = singleDashboardProductDefinition.id,
      productDefinitionName = singleDashboardProductDefinition.name,
      reportOrDashboardId = singleDashboardProductDefinition.dashboard.id,
      reportOrDashboardName = singleDashboardProductDefinition.dashboard.name,
    )
    assertEquals(statementExecutionResponse, actual)
  }

  @Test
  fun `should deduplicate prompts when calling the AthenaApiRepository to execute a report multiphase query`() {
    val reportId = "missing-ethnicity-metrics"
    val productDefinitionRepository: ProductDefinitionRepository = mock<ProductDefinitionRepository>()
    val singleReportProductDefinition = mock<SingleReportProductDefinition>()
    val report = mock<Report>()
    val specification = mock<Specification>()
    val reportDataset = mock<Dataset>()
    val query = ""
    val datasource = mock<Datasource>()
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val executionId = UUID.randomUUID().toString()
    val tableId = executionId.replace("-", "_")
    val statementExecutionResponse = StatementExecutionResponse(tableId, executionId)
    val parameterName = "establishment_code"
    val parameterValue = "BFI"
    val parameter = Parameter(0, parameterName, ParameterType.String, FilterType.AutoComplete, "Estabishment Code", true, ReferenceType.ESTABLISHMENT)
    val multiphaseQuery1 = MultiphaseQuery(0, datasource, "SELECT * FROM a", listOf(parameter))
    val multiphaseQuery2 = MultiphaseQuery(1, datasource, "SELECT * FROM b", listOf(parameter))
    val caseload = "caseloadA"
    val prompt = Prompt(parameterName, parameterValue, FilterType.AutoComplete)
    whenever(
      productDefinitionRepository.getSingleReportProductDefinition(
        definitionId = reportId,
        reportId = reportVariantId,
      ),
    ).thenReturn(singleReportProductDefinition)
    whenever(singleReportProductDefinition.report).thenReturn(report)
    whenever(singleReportProductDefinition.id).thenReturn(reportId)
    whenever(singleReportProductDefinition.name).thenReturn("name")
    whenever(report.id).thenReturn(reportVariantId)
    whenever(report.specification).thenReturn(specification)
    whenever(singleReportProductDefinition.name).thenReturn("name2")
    whenever(report.filter).thenReturn(mock<ReportFilter>())
    whenever(singleReportProductDefinition.reportDataset).thenReturn(reportDataset)
    whenever(reportDataset.query).thenReturn(query)
    whenever(reportDataset.multiphaseQuery).thenReturn(listOf(multiphaseQuery1, multiphaseQuery2))
    whenever(singleReportProductDefinition.allDatasets).thenReturn(listOf(reportDataset))
    whenever(singleReportProductDefinition.datasource).thenReturn(datasource)
    whenever(datasource.name).thenReturn("NOMIS")
    whenever(authToken.getActiveCaseLoadId()).thenReturn(caseload)
    whenever(authToken.getCaseLoadIds()).thenReturn(listOf(caseload))
    whenever(authToken.getRoles()).thenReturn(listOf("ROLE_PRISONS_REPORTING_USER"))
    val policyEngineResult = Policy.PolicyResult.POLICY_DENY
    whenever(
      athenaApiRepository.executeQueryAsync(
        filters = emptyList(),
        sortedAsc = false,
        policyEngineResult = policyEngineResult,
        prompts = listOf(prompt),
        userToken = authToken,
        query = singleReportProductDefinition.reportDataset.query,
        multiphaseQueries = listOf(multiphaseQuery1, multiphaseQuery2),
        reportFilter = singleReportProductDefinition.report.filter,
        datasource = singleReportProductDefinition.datasource,
        reportSummaries = emptyList(),
        allDatasets = singleReportProductDefinition.allDatasets,
        productDefinitionId = singleReportProductDefinition.id,
        productDefinitionName = singleReportProductDefinition.name,
        reportOrDashboardId = singleReportProductDefinition.report.id,
        reportOrDashboardName = singleReportProductDefinition.report.name,
      ),
    ).thenReturn(statementExecutionResponse)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.validateAndExecuteStatementAsync(
      reportId = reportId,
      reportVariantId = reportVariantId,
      filters = mapOf(parameterName to parameterValue),
      sortColumn = null,
      sortedAsc = true,
      userToken = authToken,
    )

    verify(athenaApiRepository, times(1)).executeQueryAsync(
      filters = emptyList(),
      sortedAsc = false,
      policyEngineResult = policyEngineResult,
      prompts = listOf(prompt),
      userToken = authToken,
      query = singleReportProductDefinition.reportDataset.query,
      multiphaseQueries = listOf(multiphaseQuery1, multiphaseQuery2),
      reportFilter = singleReportProductDefinition.report.filter,
      datasource = singleReportProductDefinition.datasource,
      reportSummaries = emptyList(),
      allDatasets = singleReportProductDefinition.allDatasets,
      productDefinitionId = singleReportProductDefinition.id,
      productDefinitionName = singleReportProductDefinition.name,
      reportOrDashboardId = singleReportProductDefinition.report.id,
      reportOrDashboardName = singleReportProductDefinition.report.name,
    )
    assertEquals(statementExecutionResponse, actual)
  }

  @ParameterizedTest
  @ValueSource(strings = ["productDefinitionNomis.json", "productDefinitionBodmis.json"])
  fun `should call the AthenaApiRepository for nomis and bodmis with the statement execution ID when getStatementStatus is called`(definitionFile: String) {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf(definitionFile),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
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

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.getStatementStatus(statementId, "external-movements", "last-month", authToken)
    verify(athenaApiRepository, times(1)).getStatementStatus(statementId)
    verifyNoInteractions(redshiftDataApiRepository)
    assertEquals(statementExecutionStatus, actual)
  }

  @ParameterizedTest
  @ValueSource(strings = ["NOMIS", "BODMIS"])
  fun `should call the AthenaApiRepository for nomis and bodmis with the statement execution ID when getDashboardStatementStatus is called`(datasourceName: String) {
    val productDefinitionRepository = mock<ProductDefinitionRepository>()
    val singleDashboardProductDefinition = mock<SingleDashboardProductDefinition>()
    val datasource = mock<Datasource>()
    val dashboardDataset = mock<Dataset>()
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val definitionId = "definitionId"
    val dashboardId = "test-dashboard"
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
      productDefinitionRepository.getSingleDashboardProductDefinition(definitionId, dashboardId),
    ).thenReturn(singleDashboardProductDefinition)
    whenever(singleDashboardProductDefinition.datasource).thenReturn(datasource)
    whenever(singleDashboardProductDefinition.dashboardDataset).thenReturn(dashboardDataset)
    whenever(datasource.name).thenReturn(datasourceName)
    whenever(
      athenaApiRepository.getStatementStatus(statementId),
    ).thenReturn(statementExecutionStatus)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.getDashboardStatementStatus(statementId, definitionId, dashboardId, authToken)
    verify(athenaApiRepository, times(1)).getStatementStatus(statementId)
    verifyNoInteractions(redshiftDataApiRepository)
    assertEquals(statementExecutionStatus, actual)
  }

  @Test
  fun `getStatementStatus should throw a MissingTableException when a tableId is provided, the table is missing and the status is FINISHED`() {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionNomis.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val statementId = "statementId"
    val tableId = TableIdGenerator().generateNewExternalTableId()
    val statementExecutionStatus = StatementExecutionStatus(
      QUERY_FINISHED,
      278109264L,
      10L,
      100L,
    )
    whenever(
      athenaApiRepository.getStatementStatus(statementId),
    ).thenReturn(statementExecutionStatus)
    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)
    whenever(
      redshiftDataApiRepository.isTableMissing(tableId),
    ).thenReturn(true)

    val exception = assertThrows<MissingTableException> {
      asyncDataApiService.getStatementStatus(
        statementId = statementId,
        reportId = "external-movements",
        reportVariantId = "last-month",
        userToken = authToken,
        tableId = tableId,
      )
    }
    assertThat(exception).message().isEqualTo("Table reports.$tableId not found.")
    verify(redshiftDataApiRepository, times(1)).isTableMissing(eq(tableId), anyOrNull())
    verify(athenaApiRepository, times(1)).getStatementStatus(eq(statementId))
  }

  @Test
  fun `isTableMissing should not be called when the status is not FINISHED`() {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionNomis.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val statementId = "statementId"
    val tableId = TableIdGenerator().generateNewExternalTableId()
    val statementExecutionStatus = StatementExecutionStatus(
      QUERY_STARTED,
      278109264L,
      10L,
      100L,
    )
    whenever(
      athenaApiRepository.getStatementStatus(statementId),
    ).thenReturn(statementExecutionStatus)
    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)
    val actual = asyncDataApiService.getStatementStatus(
      statementId = statementId,
      reportId = "external-movements",
      reportVariantId = "last-month",
      userToken = authToken,
      tableId = tableId,
    )
    assertEquals(statementExecutionStatus, actual)
    verify(redshiftDataApiRepository, times(0)).isTableMissing(any(), anyOrNull())
    verify(athenaApiRepository, times(1)).getStatementStatus(eq(statementId))
  }

  @Test
  fun `getStatementStatus should return the status when a tableId is provided and the table is not missing`() {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionNomis.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
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
    val tableId = TableIdGenerator().generateNewExternalTableId()
    whenever(
      redshiftDataApiRepository.isTableMissing(tableId),
    ).thenReturn(false)

    whenever(
      athenaApiRepository.getStatementStatus(statementId),
    ).thenReturn(statementExecutionStatus)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.getStatementStatus(
      statementId = statementId,
      reportId = "external-movements",
      reportVariantId = "last-month",
      userToken = authToken,
      tableId = tableId,
    )
    verify(athenaApiRepository, times(1)).getStatementStatus(statementId)
    verify(redshiftDataApiRepository, times(1)).isTableMissing(eq(tableId), anyOrNull())
    assertEquals(statementExecutionStatus, actual)
  }

  @Test
  fun `should call the AthenaApiRepository for datamart with the statement execution ID when report cancelStatementExecution is called`() {
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val statementId = "statementId"
    val statementCancellationResponse = StatementCancellationResponse(
      true,
    )
    whenever(
      athenaApiRepository.cancelStatementExecution(statementId),
    ).thenReturn(statementCancellationResponse)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.cancelStatementExecution(statementId, "external-movements", "last-month", authToken)
    verify(athenaApiRepository, times(1)).cancelStatementExecution(statementId)
    verifyNoInteractions(redshiftDataApiRepository)
    assertEquals(statementCancellationResponse, actual)
  }

  @Test
  fun `should call the AthenaApiRepository with the statement execution ID when cancelDashboardStatementExecution is called`() {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionWithDashboard.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val statementId = "statementId"
    val statementCancellationResponse = StatementCancellationResponse(
      true,
    )
    whenever(
      athenaApiRepository.cancelStatementExecution(statementId),
    ).thenReturn(statementCancellationResponse)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.cancelDashboardStatementExecution(statementId, "missing-ethnicity-metrics", "age-breakdown-dashboard-1", authToken)
    verify(athenaApiRepository, times(1)).cancelStatementExecution(statementId)
    verifyNoInteractions(redshiftDataApiRepository)
    assertEquals(statementCancellationResponse, actual)
  }

  @Test
  fun `should call the RedshiftDataApiRepository with the statement execution ID when getStatementStatus is called`() {
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
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

    val actual = asyncDataApiService.getStatementStatus(statementId)
    verify(redshiftDataApiRepository, times(1)).getStatementStatus(statementId)
    assertEquals(statementExecutionStatus, actual)
  }

  @Test
  fun `should throw a MissingTableException when getStatementStatus is called with a tableId, the table is missing and the status is finished`() {
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val statementExecutionStatus = StatementExecutionStatus(
      "FINISHED",
      278109264L,
      10L,
      100L,
    )
    whenever(
      redshiftDataApiRepository.getStatementStatus("statementId"),
    ).thenReturn(statementExecutionStatus)
    val tableId = TableIdGenerator().generateNewExternalTableId()
    whenever(
      redshiftDataApiRepository.isTableMissing(tableId),
    ).thenReturn(true)

    val exception = assertThrows<MissingTableException> {
      asyncDataApiService.getStatementStatus(
        statementId = "statementId",
        tableId = tableId,
      )
    }
    assertThat(exception).message().isEqualTo("Table reports.$tableId not found.")
    verify(redshiftDataApiRepository, times(1)).isTableMissing(eq(tableId), anyOrNull())
    verifyNoInteractions(athenaApiRepository)
  }

  @Test
  fun `should not call isTableMissing when getStatementStatus is called for Redshift with a tableId and the status is not finished`() {
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val statementExecutionStatus = StatementExecutionStatus(
      QUERY_STARTED,
      278109264L,
      10L,
      100L,
    )
    val statementId = "statementId"
    whenever(
      redshiftDataApiRepository.getStatementStatus(statementId),
    ).thenReturn(statementExecutionStatus)
    val tableId = TableIdGenerator().generateNewExternalTableId()

    val actual = asyncDataApiService.getStatementStatus(statementId = statementId, tableId = tableId)
    assertEquals(statementExecutionStatus, actual)
    verify(redshiftDataApiRepository, times(1)).getStatementStatus(statementId)
    verify(redshiftDataApiRepository, times(0)).isTableMissing(any(), anyOrNull())
  }

  @Test
  fun `should return the status when the Redshift getStatementStatus method is called with a tableId and the table is not missing`() {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionNomis.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
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
    val tableId = TableIdGenerator().generateNewExternalTableId()
    whenever(
      redshiftDataApiRepository.isTableMissing(tableId),
    ).thenReturn(false)

    whenever(
      redshiftDataApiRepository.getStatementStatus(statementId),
    ).thenReturn(statementExecutionStatus)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.getStatementStatus(
      statementId = statementId,
      tableId = tableId,
    )
    verify(redshiftDataApiRepository, times(1)).getStatementStatus(statementId)
    verify(redshiftDataApiRepository, times(1)).isTableMissing(eq(tableId), anyOrNull())
    assertEquals(statementExecutionStatus, actual)
  }

  @ParameterizedTest
  @ValueSource(strings = ["productDefinitionNomis.json", "productDefinitionBodmis.json"])
  fun `should call the AthenaApiRepository for nomis and bodmis with the statement execution ID when cancelStatementExecution is called`(definitionFile: String) {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf(definitionFile),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val statementId = "statementId"
    val statementCancellationResponse = StatementCancellationResponse(true)
    whenever(
      athenaApiRepository.cancelStatementExecution(statementId),
    ).thenReturn(statementCancellationResponse)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.cancelStatementExecution(statementId, "external-movements", "last-month", authToken)
    verify(athenaApiRepository, times(1)).cancelStatementExecution(statementId)
    verifyNoInteractions(redshiftDataApiRepository)
    assertEquals(statementCancellationResponse, actual)
  }

  @ParameterizedTest
  @ValueSource(strings = ["NOMIS", "BODMIS"])
  fun `should call the AthenaApiRepository for nomis and bodmis with the statement execution ID when cancelDashboardStatementExecution is called`(datasourceName: String) {
    val productDefinitionRepository = mock<ProductDefinitionRepository>()
    val singleReportProductDefinition = mock<SingleDashboardProductDefinition>()
    val datasource = mock<Datasource>()
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val definitionId = "definitionId"
    val dashboardId = "test-dashboard"
    val statementId = "statementId"
    val statementCancellationResponse = StatementCancellationResponse(true)
    whenever(
      productDefinitionRepository.getSingleDashboardProductDefinition(definitionId, dashboardId),
    ).thenReturn(singleReportProductDefinition)
    whenever(singleReportProductDefinition.datasource).thenReturn(datasource)
    whenever(datasource.name).thenReturn(datasourceName)
    whenever(
      athenaApiRepository.cancelStatementExecution(statementId),
    ).thenReturn(statementCancellationResponse)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.cancelDashboardStatementExecution(statementId, definitionId, dashboardId, authToken)
    verify(athenaApiRepository, times(1)).cancelStatementExecution(statementId)
    verifyNoInteractions(redshiftDataApiRepository)
    assertEquals(statementCancellationResponse, actual)
  }

  @Test
  fun `validateAndExecuteStatementAsync should not fail validation for filters which were converted from DPD parameters and convert these filters to prompts`() {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionWithParameters.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )
    val productDefinition = productDefinitionRepository.getProductDefinitions().first()
    val singleReportProductDefinition = productDefinitionRepository.getSingleReportProductDefinition(productDefinition.id, productDefinition.report.first().id)
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val parameter1Name = "establishment_code"
    val parameter1Value = "BFI"
    val parameter2Name = "wing"
    val parameter2Value = "BFI-A"
    val filters = mapOf(
      "origin" to "someOrigin",
      parameter1Name to parameter1Value,
      parameter2Name to parameter2Value,
    )
    val repositoryFilters = listOf(Filter("origin", "someOrigin", STANDARD))
    val prompts = listOf(
      Prompt(parameter1Name, parameter1Value, FilterType.AutoComplete),
      Prompt(parameter2Name, parameter2Value, FilterType.AutoComplete),
    )
    val sortColumn = "date"
    val sortedAsc = true
    val executionId = UUID.randomUUID().toString()
    val tableId = executionId.replace("-", "_")
    val statementExecutionResponse = StatementExecutionResponse(tableId, executionId)
    whenever(
      athenaApiRepository.executeQueryAsync(
        filters = repositoryFilters,
        sortColumn = sortColumn,
        sortedAsc = sortedAsc,
        policyEngineResult = policyEngineResultTrue,
        prompts = prompts,
        userToken = authToken,
        query = singleReportProductDefinition.reportDataset.query,
        reportFilter = singleReportProductDefinition.report.filter,
        datasource = singleReportProductDefinition.datasource,
        reportSummaries = singleReportProductDefinition.report.summary,
        allDatasets = singleReportProductDefinition.allDatasets,
        productDefinitionId = singleReportProductDefinition.id,
        productDefinitionName = singleReportProductDefinition.name,
        reportOrDashboardId = singleReportProductDefinition.report.id,
        reportOrDashboardName = singleReportProductDefinition.report.name,
      ),
    ).thenReturn(statementExecutionResponse)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.validateAndExecuteStatementAsync("external-movements-with-parameters", reportVariantId, filters, sortColumn, sortedAsc, authToken)

    verify(athenaApiRepository, times(1)).executeQueryAsync(
      filters = repositoryFilters,
      sortColumn = sortColumn,
      sortedAsc = sortedAsc,
      policyEngineResult = policyEngineResultTrue,
      prompts = prompts,
      userToken = authToken,
      query = singleReportProductDefinition.reportDataset.query,
      reportFilter = singleReportProductDefinition.report.filter,
      datasource = singleReportProductDefinition.datasource,
      reportSummaries = singleReportProductDefinition.report.summary,
      allDatasets = singleReportProductDefinition.allDatasets,
      productDefinitionId = singleReportProductDefinition.id,
      productDefinitionName = singleReportProductDefinition.name,
      reportOrDashboardId = singleReportProductDefinition.report.id,
      reportOrDashboardName = singleReportProductDefinition.report.name,
    )
    assertEquals(statementExecutionResponse, actual)
  }

  @Test
  fun `should call the repository with all provided arguments when getStatementResult is called`() {
    val tableId = TableIdGenerator().generateNewExternalTableId()
    val selectedPage = 1L
    val pageSize = 20L
    val sortColumn = "date"
    val sortedAsc = true

    whenever(
      redshiftDataApiRepository.getPaginatedExternalTableResult(
        tableId = any(),
        selectedPage = any(),
        pageSize = any(),
        filters = any(),
        sortedAsc = any(),
        sortColumn = anyOrNull(),
        jdbcTemplate = anyOrNull(),
      ),
    )
      .thenReturn(expectedRepositoryResult)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = configuredApiService.getStatementResult(
      tableId,
      reportId,
      reportVariantId,
      selectedPage = selectedPage,
      pageSize = pageSize,
      filters = mapOf("direction" to "in"),
      sortedAsc = sortedAsc,
      sortColumn = sortColumn,
      userToken = authToken,
    )

    assertEquals(expectedServiceResult, actual)

    verify(redshiftDataApiRepository).getPaginatedExternalTableResult(
      tableId = tableId,
      selectedPage = selectedPage,
      pageSize = pageSize,
      filters = listOf(Filter("direction", "in")),
      sortedAsc = sortedAsc,
      sortColumn = sortColumn,
    )
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
      identifiedHelper = IdentifiedHelper(),
    )
    val asyncDataApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val executionID = UUID.randomUUID().toString()
    whenever(
      redshiftDataApiRepository.getPaginatedExternalTableResult(executionID, selectedPage, pageSize, emptyList(), false, "date"),
    ).thenReturn(expectedRepositoryResult)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = asyncDataApiService.getStatementResult(
      tableId = executionID,
      reportId = reportId,
      reportVariantId = reportVariantId,
      selectedPage = selectedPage,
      pageSize = pageSize,
      filters = emptyMap(),
      sortedAsc = false,
      userToken = authToken,
    )
    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `should call the repository with all provided arguments when getDashboardStatementResult is called`() {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionWithDashboard.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )
    val expectedRepositoryResult = listOf(
      mapOf(
        "ESTABLISHMENT_ID" to "1",
        "HAS_ETHNICITY" to "100",
        "ETHNICITY_IS_MISSING" to "30",
      ),
    )
    val expectedServiceResult = listOf(
      listOf(
        mapOf(
          "establishment_id" to MetricData("1"),
          "has_ethnicity" to MetricData("100"),
          "ethnicity_is_missing" to MetricData("30"),
        ),
      ),
    )
    val configuredApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val tableId = TableIdGenerator().generateNewExternalTableId()
    val selectedPage = 1L
    val pageSize = 20L
    whenever(
      redshiftDataApiRepository
        .getPaginatedExternalTableResult(tableId, selectedPage, pageSize, emptyList()),
    ).thenReturn(expectedRepositoryResult)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = configuredApiService.getDashboardStatementResult(
      tableId,
      "missing-ethnicity-metrics",
      "age-breakdown-dashboard-1",
      selectedPage = selectedPage,
      pageSize = pageSize,
      filters = emptyMap(),
      userToken = authToken,
    )

    assertEquals(expectedServiceResult, actual)
  }

  @Test
  fun `getDashboardStatementResult throws an exception when the result columns are not in the dataset schema`() {
    val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionWithDashboard.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )
    val expectedRepositoryResult = listOf(
      mapOf(
        "ESTABLISHMENT_ID" to "1",
        "HAS_ETHNICITY" to "100",
        "ETHNICITY_IS_MISSING" to "30",
        "RANDOM_ROW" to "abc",
      ),
    )
    val configuredApiService = AsyncDataApiService(productDefinitionRepository, configuredApiRepository, redshiftDataApiRepository, athenaApiRepository, tableIdGenerator, identifiedHelper, productDefinitionTokenPolicyChecker)
    val tableId = TableIdGenerator().generateNewExternalTableId()
    val selectedPage = 1L
    val pageSize = 20L
    whenever(
      redshiftDataApiRepository.getPaginatedExternalTableResult(tableId, selectedPage, pageSize, emptyList()),
    ).thenReturn(expectedRepositoryResult)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val exception = Assertions.assertThrows(ValidationException::class.java) {
      configuredApiService.getDashboardStatementResult(
        tableId,
        "missing-ethnicity-metrics",
        "age-breakdown-dashboard-1",
        selectedPage = selectedPage,
        pageSize = pageSize,
        filters = emptyMap(),
        userToken = authToken,
      )
    }
    assertThat(exception).message().isEqualTo("The DPD is missing schema field: RANDOM_ROW.")
  }

  @Test
  fun `should call the repository with all provided arguments when getSummaryResult is called`() {
    val tableId = TableIdGenerator().generateNewExternalTableId()
    val summaryId = "summaryId"
    whenever(
      redshiftDataApiRepository.getFullExternalTableResult(tableIdGenerator.getTableSummaryId(tableId, summaryId)),
    ).thenReturn(listOf(mapOf("TOTAL" to 1)))

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = configuredApiService.getSummaryResult(
      tableId,
      summaryId,
      reportId,
      reportVariantId,
      filters = emptyMap(),
      userToken = authToken,
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

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = configuredApiService.getSummaryResult(
      tableId,
      summaryId,
      reportId,
      reportVariantId,
      filters = emptyMap(),
      userToken = authToken,
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

  @Test
  fun `should call the repository with all provided arguments when the interactive count is called`() {
    val tableId = "123"
    val filters = mapOf("direction" to "in")
    val expectedRepositoryResult = 5L
    whenever(
      redshiftDataApiRepository.count(tableId, listOf(Filter("direction", "in"))),
    ).thenReturn(expectedRepositoryResult)

    whenever(
      productDefinitionTokenPolicyChecker.determineAuth(
        withPolicy = any(),
        userToken = any(),
      ),
    ).thenReturn(true)

    val actual = configuredApiService.count(tableId, "external-movements", "last-month", filters, authToken)

    assertEquals(Count(expectedRepositoryResult), actual)
  }

  @Test
  fun `should generate correct tableId for cached scheduled dataset`() {
    val definition = this.definition(
      scheduled = false,
      dataset = dataset(),
    )

    val actual = configuredApiService.generateScheduledDatasetId(definition)
    assertEquals("_MToxMA__", actual)
  }

  @Test
  fun `should not return table id if definition not scheduled`() {
    val definitionWithNoSchedule = definition(
      scheduled = false,
      dataset = dataset(),
    )

    val actual = configuredApiService.checkForScheduledDataset(definitionWithNoSchedule)
    assertTrue(actual == null)
  }

  @Test
  fun `should return table id if definition scheduled and dataset available`() {
    val definitionWithSchedule = definition(
      scheduled = true,
      dataset = dataset("0 15 10 ? * MON-FRI"),
    )
    val tableId = "_MToxMA__"
    val actual = configuredApiService.checkForScheduledDataset(definitionWithSchedule)

    assertEquals(tableId, actual)
  }

  private fun dataset(schedule: String? = null): Dataset = Dataset(
    id = "10",
    name = "11",
    datasource = "12A",
    query = "12",
    schema = Schema(
      field = listOf(
        SchemaField(
          name = "13",
          type = ParameterType.Long,
          display = "14",
          filter = null,
        ),
      ),
    ),
    schedule = schedule,
  )

  private fun definition(scheduled: Boolean, dataset: Dataset): SingleReportProductDefinition {
    val fullDatasource = Datasource(
      id = "18",
      name = "19",
    )
    val fullReport = Mockito.mock<Report>()
    return SingleReportProductDefinition(
      id = "1",
      name = "2",
      description = "3",
      scheduled = scheduled,
      metadata = MetaData(
        author = "4",
        version = "5",
        owner = "6",
        purpose = "7",
        profile = "8",
      ),
      reportDataset = dataset,
      datasource = fullDatasource,
      report = fullReport,
      policy = listOf(
        Policy(
          id = "caseload",
          type = PolicyType.ACCESS,
          rule = listOf(Rule(Effect.PERMIT, emptyList())),
        ),
      ),
      allDatasets = listOf(dataset),
      allReports = emptyList(),
    )
  }
}
