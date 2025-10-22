package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.validation.ValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse
import software.amazon.awssdk.services.athena.model.QueryExecution
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse
import software.amazon.awssdk.services.athena.model.StopQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StopQueryExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.CONTEXT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.DEFAULT_REPORT_CTE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.FALSE_WHERE_CLAUSE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.FILTER_
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.POLICY_
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.PROMPT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.REPORT_
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.TRUE_WHERE_CLAUSE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.DatasourceConnection
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MultiphaseQuery
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportFilter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SqlDialect
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_DENY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult.POLICY_PERMIT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementCancellationResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Prompt
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class AthenaApiRepositoryTest {

  companion object {
    val tableId = "_a6227417_bdac_40bb_bc81_49c750daacd7"
    val executionId = "someId"
    val testDb = "testdb"
    val testCatalog = "testcatalog"
    val athenaWorkgroup = "athenaWorkgroup"
    val dpdQuery = "SELECT column_a,column_b FROM schema_a.table_a"
    val defaultDatasetCte = "dataset_ AS (SELECT column_a,column_b FROM schema_a.table_a)"
    val emptyPromptsCte = "$PROMPT AS (SELECT '''' FROM DUAL)"
    private val testUsername = "aUser"
    private val testCaseload = "aCaseload"
    private val testAccountType = "GENERAL"
    private val contextCte = """WITH $CONTEXT AS (
      SELECT 
      ''$testUsername'' AS username, 
      ''$testCaseload'' AS caseload, 
      ''$testAccountType'' AS account_type 
      FROM DUAL
      )"""
  }
  fun sqlStatement(
    tableId: String,
    whereClauseCondition: String? = TRUE_WHERE_CLAUSE,
    promptsCte: String? = emptyPromptsCte,
    datasetCte: String? = defaultDatasetCte,
    prefilter: ReportFilter? = ReportFilter(name = REPORT_, query = DEFAULT_REPORT_CTE),
  ) = """          /* dpdId dpdName reportId reportName */
          CREATE TABLE AwsDataCatalog.reports.$tableId 
          WITH (
            format = 'PARQUET'
          ) 
          AS (
          SELECT * FROM TABLE(system.query(query =>
           '$contextCte,$promptsCte,$datasetCte,${prefilter?.query},policy_ AS (SELECT * FROM ${prefilter?.name} WHERE $whereClauseCondition),$FILTER_ AS (SELECT * FROM $POLICY_ WHERE $TRUE_WHERE_CLAUSE)
SELECT *
          FROM $FILTER_ ORDER BY column_a asc'
           )) 
          );
  """.trimIndent()

  private fun multiphaseSqlNonLastQuery() = """          /* dpdId dpdName reportId reportName */
          CREATE TABLE AwsDataCatalog.reports._a6227417_bdac_40bb_bc81_49c750daacd7 
          WITH (
            format = 'PARQUET'
          ) 
          AS (
          SELECT * FROM TABLE(system.query(query =>
           'WITH context_ AS (
      SELECT 
      ''aUser'' AS username, 
      ''aCaseload'' AS caseload, 
      ''GENERAL'' AS account_type 
      FROM DUAL
      ),prompt_ AS (SELECT '''' FROM DUAL),dataset_ AS (SELECT column_a,column_b FROM schema_a.table_a)
SELECT * FROM dataset_'
           )) 
          );"""

  private val athenaClient = mock<AthenaClient>()
  private val tableIdGenerator = mock<TableIdGenerator>()
  private val productDefinition = mock<SingleReportProductDefinition>()
  private val jdbcTemplate = mock<JdbcTemplate>()
  private val athenaApiRepository = AthenaApiRepository(
    athenaClient,
    tableIdGenerator,
    athenaWorkgroup,
    jdbcTemplate,
  )
  private val startQueryExecutionResponse = mock<StartQueryExecutionResponse>()
  private val dataset = mock<Dataset>()
  private val datasource = mock<Datasource>()
  private val report = mock<Report>()
  private val userToken = mock<DprAuthAwareAuthenticationToken>()

  @ParameterizedTest
  @CsvSource(
    "$POLICY_PERMIT, $TRUE_WHERE_CLAUSE",
    "${POLICY_DENY}, $FALSE_WHERE_CLAUSE",
  )
  fun `executeQueryAsync should call the athena data api with the correct query which includes the context_ cte and return the execution id and table id`(policyEngineResult: String, whereClauseCondition: String) {
    val startQueryExecutionRequest = setupBasicMocks(whereClause = whereClauseCondition)
    whenever(dataset.query).thenReturn(dpdQuery)
    val actual = athenaApiRepository.executeQueryAsync(
      filters = emptyList(),
      sortColumn = "column_a",
      sortedAsc = true,
      policyEngineResult = policyEngineResult,
      userToken = userToken,
      query = productDefinition.reportDataset.query,
      reportFilter = productDefinition.report.filter,
      datasource = productDefinition.datasource,
      reportSummaries = productDefinition.report.summary,
      allDatasets = productDefinition.allDatasets,
      productDefinitionId = productDefinition.id,
      productDefinitionName = productDefinition.name,
      reportOrDashboardId = productDefinition.report.id,
      reportOrDashboardName = productDefinition.report.name,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
  }

  @Test
  fun `executeQueryAsync should map prompts to the prompt_ CTE correctly`() {
    val startQueryExecutionRequest = setupBasicMocks(promptsCte = "$PROMPT AS (SELECT ''filterValue1'' AS filterName1, ''filterValue2'' AS filterName2 FROM DUAL)")
    val prompts = listOf(Prompt("filterName1", "filterValue1", FilterType.Text), Prompt("filterName2", "filterValue2", FilterType.Text))
    whenever(dataset.query).thenReturn(defaultDatasetCte)
    val actual = athenaApiRepository.executeQueryAsync(
      filters = emptyList(),
      sortColumn = "column_a",
      sortedAsc = true,
      policyEngineResult = POLICY_PERMIT,
      prompts = prompts,
      userToken = userToken,
      query = productDefinition.reportDataset.query,
      reportFilter = productDefinition.report.filter,
      datasource = productDefinition.datasource,
      reportSummaries = productDefinition.report.summary,
      allDatasets = productDefinition.allDatasets,
      productDefinitionId = productDefinition.id,
      productDefinitionName = productDefinition.name,
      reportOrDashboardId = productDefinition.report.id,
      reportOrDashboardName = productDefinition.report.name,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
  }

  @Test
  fun `executeQueryAsync should map prompts to the prompt_ CTE correctly for date prompts`() {
    val startQueryExecutionRequest = setupBasicMocks(promptsCte = "$PROMPT AS (SELECT TO_DATE(''01/01/2023'',''yyyy-mm-dd'') AS start_date FROM DUAL)")
    val prompts = listOf(Prompt("start_date", "01/01/2023", FilterType.Date))
    whenever(dataset.query).thenReturn(defaultDatasetCte)
    val actual = athenaApiRepository.executeQueryAsync(
      filters = emptyList(),
      sortColumn = "column_a",
      sortedAsc = true,
      policyEngineResult = POLICY_PERMIT,
      prompts = prompts,
      userToken = userToken,
      query = productDefinition.reportDataset.query,
      reportFilter = productDefinition.report.filter,
      datasource = productDefinition.datasource,
      reportSummaries = productDefinition.report.summary,
      allDatasets = productDefinition.allDatasets,
      productDefinitionId = productDefinition.id,
      productDefinitionName = productDefinition.name,
      reportOrDashboardId = productDefinition.report.id,
      reportOrDashboardName = productDefinition.report.name,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
  }

  @Test
  fun `executeQueryAsync should use the existing main report query when the dataset_ CTE is already embedded into the query`() {
    val dpdQuery = "dataset_ as (SELECT column_c,column_d FROM schema_a.table_a)"
    val startQueryExecutionRequest = setupBasicMocks(datasetCte = dpdQuery)

    whenever(dataset.query).thenReturn(dpdQuery)
    val actual = athenaApiRepository.executeQueryAsync(
      filters = emptyList(),
      sortColumn = "column_a",
      sortedAsc = true,
      policyEngineResult = POLICY_PERMIT,
      userToken = userToken,
      query = productDefinition.reportDataset.query,
      reportFilter = productDefinition.report.filter,
      datasource = productDefinition.datasource,
      reportSummaries = productDefinition.report.summary,
      allDatasets = productDefinition.allDatasets,
      productDefinitionId = productDefinition.id,
      productDefinitionName = productDefinition.name,
      reportOrDashboardId = productDefinition.report.id,
      reportOrDashboardName = productDefinition.report.name,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
  }

  @Test
  fun `executeQueryAsync should use add the report filter CTE to the final query when it exists`() {
    val reportFilter = ReportFilter(name = "someprefiltername_", query = "someprefiltername_ AS (SELECT a,b FROM dataset_)")
    val startQueryExecutionRequest = setupBasicMocks(reportFilter = reportFilter)

    whenever(dataset.query).thenReturn(dpdQuery)
    val actual = athenaApiRepository.executeQueryAsync(
      filters = emptyList(),
      sortColumn = "column_a",
      sortedAsc = true,
      policyEngineResult = POLICY_PERMIT,
      userToken = userToken,
      query = productDefinition.reportDataset.query,
      reportFilter = productDefinition.report.filter,
      datasource = productDefinition.datasource,
      reportSummaries = productDefinition.report.summary,
      allDatasets = productDefinition.allDatasets,
      productDefinitionId = productDefinition.id,
      productDefinitionName = productDefinition.name,
      reportOrDashboardId = productDefinition.report.id,
      reportOrDashboardName = productDefinition.report.name,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
  }

  @ParameterizedTest
  @CsvSource(
    "QUEUED, SUBMITTED",
    "RUNNING, STARTED",
    "SUCCEEDED, FINISHED",
    "CANCELLED, ABORTED",
  )
  fun `getStatementStatus should call the getQueryExecution athena api with the correct statement ID and return the StatementExecutionStatus mapped correctly`(athenaStatus: String, redshiftStatus: String) {
    val query = sqlStatement(tableId = "tableId")
    val statementId = "statementId"
    val getQueryExecutionRequest = GetQueryExecutionRequest.builder()
      .queryExecutionId(statementId)
      .build()
    val completionTime = Instant.now()
    val submissionTime = completionTime.minus(Duration.of(10, ChronoUnit.MINUTES))
    val getQueryExecutionResponse = GetQueryExecutionResponse.builder()
      .queryExecution(
        QueryExecution.builder()
          .query(query)
          .status(
            QueryExecutionStatus.builder().state(
              athenaStatus,
            )
              .submissionDateTime(submissionTime)
              .completionDateTime(completionTime)
              .build(),
          ).build(),
      ).build()
    whenever(
      athenaClient.getQueryExecution(
        getQueryExecutionRequest,
      ),
    ).thenReturn(getQueryExecutionResponse)
    val tenMinutesInNanoseconds = 600000000000
    val expected = StatementExecutionStatus(
      redshiftStatus,
      tenMinutesInNanoseconds,
      0L,
      0L,
    )
    val actual = athenaApiRepository.getStatementStatus(statementId)

    assertEquals(expected, actual)
  }

  @ParameterizedTest
  @CsvSource(
    "$QUERY_SUCCEEDED, $QUERY_FINISHED",
    "$QUERY_FAILED, $QUERY_FAILED",
    "$QUERY_QUEUED, $QUERY_SUBMITTED",
    "$QUERY_CANCELLED, $QUERY_ABORTED",
  )
  fun `getStatementStatusForMultiphaseQuery should return the StatementExecutionStatus of one multiphase query mapped correctly`(athenaStatus: String, mappedRedshiftStatus: String) {
    val jdbcTemplate = mock<NamedParameterJdbcTemplate>()
    val statementId = "statementId"
    val errorMessageBase64Enc = "ZXhjZXB0aW9u"
    val errorMessageBase64Dec = "exception"
    val queryResults: MutableList<Map<String, Any?>> = mutableListOf()
    if (athenaStatus == QUERY_FAILED) {
      queryResults.add(mapOf(CURRENT_STATE_COL to athenaStatus, ERROR_COL to errorMessageBase64Enc, INDEX_COL to 0))
    } else {
      queryResults.add(mapOf(CURRENT_STATE_COL to athenaStatus, INDEX_COL to 0))
    }
    val mapSqlParameterSource = MapSqlParameterSource()
    mapSqlParameterSource.addValue("rootExecutionId", statementId)

    whenever(
      jdbcTemplate.queryForList(
        anyString(),
        any(MapSqlParameterSource::class.java),
      ),
    ).thenReturn(queryResults)

    val expected = if (athenaStatus == QUERY_FAILED) {
      StatementExecutionStatus(
        status = mappedRedshiftStatus,
        duration = 1,
        resultRows = 0L,
        resultSize = 0L,
        error = errorMessageBase64Dec,
        stateChangeReason = errorMessageBase64Dec,
      )
    } else {
      StatementExecutionStatus(
        status = mappedRedshiftStatus,
        duration = 1,
        resultRows = 0L,
        resultSize = 0L,
      )
    }

    val actual = athenaApiRepository.getStatementStatusForMultiphaseQuery(statementId, jdbcTemplate)

    val stringArgumentCaptor = argumentCaptor<String>()
    val mapSqlArgumentCaptor = argumentCaptor<MapSqlParameterSource>()
    verify(jdbcTemplate, times(1)).queryForList(
      stringArgumentCaptor.capture(),
      mapSqlArgumentCaptor.capture(),
    )
    assertEquals(stringArgumentCaptor.firstValue, "SELECT $INDEX_COL, $CURRENT_STATE_COL, $ERROR_COL FROM admin.multiphase_query_state WHERE $ROOT_EXECUTION_ID_COL = :rootExecutionId;")
    assertEquals(mapSqlArgumentCaptor.firstValue.toString(), mapSqlParameterSource.toString())
    assertEquals(expected, actual)
  }

  @ParameterizedTest
  @CsvSource(
    "$QUERY_SUCCEEDED, $QUERY_QUEUED, $QUERY_SUBMITTED",
    "$QUERY_SUCCEEDED, $QUERY_RUNNING, $QUERY_STARTED",
    "$QUERY_SUCCEEDED, $QUERY_SUCCEEDED, $QUERY_FINISHED",
    "$QUERY_SUCCEEDED, $QUERY_CANCELLED, $QUERY_ABORTED",
    "$QUERY_FAILED, $QUERY_SUCCEEDED, $QUERY_FAILED",
    "$QUERY_CANCELLED, $QUERY_SUCCEEDED, $QUERY_ABORTED",
    "$QUERY_CANCELLED,, $QUERY_ABORTED",
    "$QUERY_FAILED,, $QUERY_FAILED",
    "$QUERY_QUEUED,, $QUERY_SUBMITTED",
    "$QUERY_RUNNING,, $QUERY_SUBMITTED",
    "$QUERY_SUCCEEDED,, $QUERY_SUBMITTED",
  )
  fun `getStatementStatusForMultiphaseQuery should calculate the aggregate status of all multiphase queries and return the StatementExecutionStatus mapped correctly`(
    athenaFirstStatus: String,
    athenaSecondStatus: String?,
    mappedRedshiftStatus: String,
  ) {
    val jdbcTemplate = mock<NamedParameterJdbcTemplate>()
    val statementId = "statementId"
    val errorMessageBase64Enc = "ZXhjZXB0aW9u"
    val errorMessageBase64Dec = "exception"
    val queryResults: MutableList<Map<String, Any?>> = mutableListOf()
    if (athenaFirstStatus == QUERY_FAILED) {
      queryResults.add(mapOf(CURRENT_STATE_COL to athenaFirstStatus, ERROR_COL to errorMessageBase64Enc, INDEX_COL to 0))
      queryResults.add(mapOf(CURRENT_STATE_COL to athenaSecondStatus, INDEX_COL to 1))
    } else if (athenaSecondStatus == QUERY_FAILED) {
      queryResults.add(mapOf(CURRENT_STATE_COL to athenaFirstStatus, INDEX_COL to 0))
      queryResults.add(mapOf(CURRENT_STATE_COL to athenaSecondStatus, ERROR_COL to errorMessageBase64Enc, INDEX_COL to 1))
    } else {
      queryResults.add(mapOf(CURRENT_STATE_COL to athenaFirstStatus, INDEX_COL to 0))
      queryResults.add(mapOf(CURRENT_STATE_COL to athenaSecondStatus, INDEX_COL to 1))
    }
    val expected = if (athenaFirstStatus == QUERY_FAILED || athenaSecondStatus == QUERY_FAILED) {
      StatementExecutionStatus(
        status = mappedRedshiftStatus,
        duration = 1,
        resultRows = 0L,
        resultSize = 0L,
        error = errorMessageBase64Dec,
        stateChangeReason = errorMessageBase64Dec,
      )
    } else {
      StatementExecutionStatus(
        status = mappedRedshiftStatus,
        duration = 1,
        resultRows = 0L,
        resultSize = 0L,
      )
    }
    val mapSqlParameterSource = MapSqlParameterSource()
    mapSqlParameterSource.addValue("rootExecutionId", statementId)

    whenever(
      jdbcTemplate.queryForList(
        anyString(),
        any(MapSqlParameterSource::class.java),
      ),
    ).thenReturn(queryResults)

    val actual = athenaApiRepository.getStatementStatusForMultiphaseQuery(statementId, jdbcTemplate)

    val stringArgumentCaptor = argumentCaptor<String>()
    val mapSqlArgumentCaptor = argumentCaptor<MapSqlParameterSource>()
    verify(jdbcTemplate, times(1)).queryForList(
      stringArgumentCaptor.capture(),
      mapSqlArgumentCaptor.capture(),
    )
    assertEquals(stringArgumentCaptor.firstValue, "SELECT $INDEX_COL, $CURRENT_STATE_COL, $ERROR_COL FROM admin.multiphase_query_state WHERE $ROOT_EXECUTION_ID_COL = :rootExecutionId;")
    assertEquals(mapSqlArgumentCaptor.firstValue.toString(), mapSqlParameterSource.toString())
    assertEquals(expected, actual)
  }

  @Test
  fun `cancelStatementExecution should call the stopQueryExecution athena api with the correct statement ID and return a successful StatementCancellationResponse`() {
    val statementId = "statementId"
    val stopQueryExecutionRequest = StopQueryExecutionRequest.builder()
      .queryExecutionId(statementId)
      .build()
    val stopQueryExecutionResponse = StopQueryExecutionResponse.builder().build()
    whenever(
      athenaClient.stopQueryExecution(
        stopQueryExecutionRequest,
      ),
    ).thenReturn(stopQueryExecutionResponse)
    val expected = StatementCancellationResponse(true)
    val actual = athenaApiRepository.cancelStatementExecution(statementId)

    assertEquals(expected, actual)
  }

  @Test
  fun `executeQueryAsync should run a multiphase query when there are two multiphase queries defined`() {
    val database = "db"
    val catalog = "catalog"
    val startQueryExecutionRequest = setupBasicMocks(
      database = database,
      catalog = catalog,
      query = multiphaseSqlNonLastQuery(),
    )
    val datasource1 = Datasource("id", "name", database, catalog)
    val datasource2 = Datasource("id2", "name2", database, catalog, DatasourceConnection.AWS_DATA_CATALOG)
    val query2 = "SELECT count(*) as total from \${table[0]}"
    val multiphaseQuery = listOf(
      MultiphaseQuery(0, datasource1, dpdQuery),
      MultiphaseQuery(1, datasource2, query2),
    )
    val tableId2 = "tableId2"

    whenever(dataset.multiphaseQuery).thenReturn(multiphaseQuery)
    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      tableId,
      tableId2,
    )
    val actual = athenaApiRepository.executeQueryAsync(
      filters = emptyList(),
      sortColumn = "column_a",
      sortedAsc = true,
      policyEngineResult = TRUE_WHERE_CLAUSE,
      userToken = userToken,
      query = "",
      reportFilter = productDefinition.report.filter,
      datasource = productDefinition.datasource,
      reportSummaries = productDefinition.report.summary,
      allDatasets = productDefinition.allDatasets,
      productDefinitionId = productDefinition.id,
      productDefinitionName = productDefinition.name,
      reportOrDashboardId = productDefinition.report.id,
      reportOrDashboardName = productDefinition.report.name,
      multiphaseQueries = multiphaseQuery,
    )
    val firstMultiphaseInsert = """insert into 
          admin.multiphase_query_state (
          root_execution_id,
          current_execution_id,
          datasource_name,
          catalog,
          database,
          index,
          query,
          sequence_number,
          last_update
          )
          values (
            'someId',
            'someId',
            'name',
            'catalog',
            'db',
            0,
            'ICAgICAgICAgIC8qIGRwZElkIGRwZE5hbWUgcmVwb3J0SWQgcmVwb3J0TmFtZSAqLwogICAgICAgICAgQ1JFQVRFIFRBQkxFIEF3c0RhdGFDYXRhbG9nLnJlcG9ydHMuX2E2MjI3NDE3X2JkYWNfNDBiYl9iYzgxXzQ5Yzc1MGRhYWNkNyAKICAgICAgICAgIFdJVEggKAogICAgICAgICAgICBmb3JtYXQgPSAnUEFSUVVFVCcKICAgICAgICAgICkgCiAgICAgICAgICBBUyAoCiAgICAgICAgICBTRUxFQ1QgKiBGUk9NIFRBQkxFKHN5c3RlbS5xdWVyeShxdWVyeSA9PgogICAgICAgICAgICdXSVRIIGNvbnRleHRfIEFTICgKICAgICAgU0VMRUNUIAogICAgICAnJ2FVc2VyJycgQVMgdXNlcm5hbWUsIAogICAgICAnJ2FDYXNlbG9hZCcnIEFTIGNhc2Vsb2FkLCAKICAgICAgJydHRU5FUkFMJycgQVMgYWNjb3VudF90eXBlIAogICAgICBGUk9NIERVQUwKICAgICAgKSxwcm9tcHRfIEFTIChTRUxFQ1QgJycnJyBGUk9NIERVQUwpLGRhdGFzZXRfIEFTIChTRUxFQ1QgY29sdW1uX2EsY29sdW1uX2IgRlJPTSBzY2hlbWFfYS50YWJsZV9hKQpTRUxFQ1QgKiBGUk9NIGRhdGFzZXRfJwogICAgICAgICAgICkpIAogICAgICAgICAgKTs=',
            0,
            SYSDATE
          )"""
    val secondMultiphaseInsert = """insert into 
          admin.multiphase_query_state (
          root_execution_id,
          
          datasource_name,
          catalog,
          database,
          index,
          query,
          sequence_number,
          last_update
          )
          values (
            'someId',
            
            'name2',
            'catalog',
            'db',
            1,
            'ICAgICAgICAgICAgLyogZHBkSWQgZHBkTmFtZSByZXBvcnRJZCByZXBvcnROYW1lICovCiAgICAgICAgICAgICAgICBDUkVBVEUgVEFCTEUgQXdzRGF0YUNhdGFsb2cucmVwb3J0cy50YWJsZUlkMgogICAgICAgICAgICAgICAgV0lUSCAoCiAgICAgICAgICAgICAgICAgIGZvcm1hdCA9ICdQQVJRVUVUJwogICAgICAgICAgICAgICAgKSAKICAgICAgICAgICAgICAgIEFTICgKICAgICAgICAgIFdJVEggY29udGV4dF8gQVMgKAogICAgICBTRUxFQ1QgCiAgICAgICdhVXNlcicgQVMgdXNlcm5hbWUsIAogICAgICAnYUNhc2Vsb2FkJyBBUyBjYXNlbG9hZCwgCiAgICAgICdHRU5FUkFMJyBBUyBhY2NvdW50X3R5cGUgCiAgICAgIAogICAgICApLHByb21wdF8gQVMgKFNFTEVDVCAnJyApLGRhdGFzZXRfIEFTIChTRUxFQ1QgY291bnQoKikgYXMgdG90YWwgZnJvbSBfYTYyMjc0MTdfYmRhY180MGJiX2JjODFfNDljNzUwZGFhY2Q3KSxyZXBvcnRfIEFTIChTRUxFQ1QgKiBGUk9NIGRhdGFzZXRfKSxwb2xpY3lfIEFTIChTRUxFQ1QgKiBGUk9NIHJlcG9ydF8gV0hFUkUgMT0xKSxmaWx0ZXJfIEFTIChTRUxFQ1QgKiBGUk9NIHBvbGljeV8gV0hFUkUgMT0xKQpTRUxFQ1QgKgogICAgICAgICAgRlJPTSBmaWx0ZXJfIE9SREVSIEJZIGNvbHVtbl9hIGFzYwogICAgICAgICAgICAgICAgKQ==',
            0,
            SYSDATE
          )"""

    verify(jdbcTemplate).execute(firstMultiphaseInsert)
    verify(jdbcTemplate).execute(secondMultiphaseInsert)
    val inOrder = inOrder(jdbcTemplate)
    inOrder.verify(jdbcTemplate).execute(firstMultiphaseInsert)
    inOrder.verify(jdbcTemplate).execute(secondMultiphaseInsert.trimIndent())
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
    assertEquals(StatementExecutionResponse(tableId2, executionId), actual)
  }

  @Test
  fun `executeQueryAsync should run a multiphase query when there are three multiphase queries defined`() {
    val database = "db"
    val catalog = "catalog"
    val startQueryExecutionRequest = setupBasicMocks(
      database = database,
      catalog = catalog,
      query = multiphaseSqlNonLastQuery(),
    )
    val datasource = Datasource("id", "name", database, catalog, DatasourceConnection.FEDERATED, dialect = SqlDialect.ORACLE11g)
    val datasource2 = Datasource("id", "name", database, catalog, DatasourceConnection.AWS_DATA_CATALOG, dialect = SqlDialect.ATHENA3)
    val datasource3 = Datasource("id", "name", database, catalog, DatasourceConnection.AWS_DATA_CATALOG, dialect = SqlDialect.ATHENA3)
    val tableId2 = "tableId2"
    val tableId3 = "tableId3"
    val query2 = "SELECT count(*) as total from \${table[0]}"
    val query3 = "SELECT count(*) + 1 as total_plus_one from \${table[1]}"
    val multiphaseQuery = listOf(
      MultiphaseQuery(0, datasource, dpdQuery),
      MultiphaseQuery(1, datasource2, query2),
      MultiphaseQuery(2, datasource3, query3),
    )
    whenever(dataset.multiphaseQuery).thenReturn(multiphaseQuery)
    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      tableId,
      tableId2,
      tableId3,
    )
    val actual = athenaApiRepository.executeQueryAsync(
      filters = emptyList(),
      sortColumn = "column_a",
      sortedAsc = true,
      policyEngineResult = TRUE_WHERE_CLAUSE,
      userToken = userToken,
      query = "",
      reportFilter = productDefinition.report.filter,
      datasource = productDefinition.datasource,
      reportSummaries = productDefinition.report.summary,
      allDatasets = productDefinition.allDatasets,
      productDefinitionId = productDefinition.id,
      productDefinitionName = productDefinition.name,
      reportOrDashboardId = productDefinition.report.id,
      reportOrDashboardName = productDefinition.report.name,
      multiphaseQueries = multiphaseQuery,
    )
    val firstMultiphaseInsert = """insert into 
          admin.multiphase_query_state (
          root_execution_id,
          current_execution_id,
          datasource_name,
          catalog,
          database,
          index,
          query,
          sequence_number,
          last_update
          )
          values (
            'someId',
            'someId',
            'name',
            'catalog',
            'db',
            0,
            'ICAgICAgICAgIC8qIGRwZElkIGRwZE5hbWUgcmVwb3J0SWQgcmVwb3J0TmFtZSAqLwogICAgICAgICAgQ1JFQVRFIFRBQkxFIEF3c0RhdGFDYXRhbG9nLnJlcG9ydHMuX2E2MjI3NDE3X2JkYWNfNDBiYl9iYzgxXzQ5Yzc1MGRhYWNkNyAKICAgICAgICAgIFdJVEggKAogICAgICAgICAgICBmb3JtYXQgPSAnUEFSUVVFVCcKICAgICAgICAgICkgCiAgICAgICAgICBBUyAoCiAgICAgICAgICBTRUxFQ1QgKiBGUk9NIFRBQkxFKHN5c3RlbS5xdWVyeShxdWVyeSA9PgogICAgICAgICAgICdXSVRIIGNvbnRleHRfIEFTICgKICAgICAgU0VMRUNUIAogICAgICAnJ2FVc2VyJycgQVMgdXNlcm5hbWUsIAogICAgICAnJ2FDYXNlbG9hZCcnIEFTIGNhc2Vsb2FkLCAKICAgICAgJydHRU5FUkFMJycgQVMgYWNjb3VudF90eXBlIAogICAgICBGUk9NIERVQUwKICAgICAgKSxwcm9tcHRfIEFTIChTRUxFQ1QgJycnJyBGUk9NIERVQUwpLGRhdGFzZXRfIEFTIChTRUxFQ1QgY29sdW1uX2EsY29sdW1uX2IgRlJPTSBzY2hlbWFfYS50YWJsZV9hKQpTRUxFQ1QgKiBGUk9NIGRhdGFzZXRfJwogICAgICAgICAgICkpIAogICAgICAgICAgKTs=',
            0,
            SYSDATE
          )"""
    val secondMultiphaseInsert = """insert into 
          admin.multiphase_query_state (
          root_execution_id,
          
          datasource_name,
          catalog,
          database,
          index,
          query,
          sequence_number,
          last_update
          )
          values (
            'someId',
            
            'name',
            'catalog',
            'db',
            1,
            'ICAgICAgICAgICAgLyogZHBkSWQgZHBkTmFtZSByZXBvcnRJZCByZXBvcnROYW1lICovCiAgICAgICAgICAgICAgICBDUkVBVEUgVEFCTEUgQXdzRGF0YUNhdGFsb2cucmVwb3J0cy50YWJsZUlkMgogICAgICAgICAgICAgICAgV0lUSCAoCiAgICAgICAgICAgICAgICAgIGZvcm1hdCA9ICdQQVJRVUVUJwogICAgICAgICAgICAgICAgKSAKICAgICAgICAgICAgICAgIEFTICgKICAgICAgICAgIFdJVEggY29udGV4dF8gQVMgKAogICAgICBTRUxFQ1QgCiAgICAgICdhVXNlcicgQVMgdXNlcm5hbWUsIAogICAgICAnYUNhc2Vsb2FkJyBBUyBjYXNlbG9hZCwgCiAgICAgICdHRU5FUkFMJyBBUyBhY2NvdW50X3R5cGUgCiAgICAgIAogICAgICApLHByb21wdF8gQVMgKFNFTEVDVCAnJyApLGRhdGFzZXRfIEFTIChTRUxFQ1QgY291bnQoKikgYXMgdG90YWwgZnJvbSBfYTYyMjc0MTdfYmRhY180MGJiX2JjODFfNDljNzUwZGFhY2Q3KQpTRUxFQ1QgKiBGUk9NIGRhdGFzZXRfCiAgICAgICAgICAgICAgICAp',
            0,
            SYSDATE
          )"""
    val thirdMultiphaseInsert = """insert into 
          admin.multiphase_query_state (
          root_execution_id,
          
          datasource_name,
          catalog,
          database,
          index,
          query,
          sequence_number,
          last_update
          )
          values (
            'someId',
            
            'name',
            'catalog',
            'db',
            2,
            'ICAgICAgICAgICAgLyogZHBkSWQgZHBkTmFtZSByZXBvcnRJZCByZXBvcnROYW1lICovCiAgICAgICAgICAgICAgICBDUkVBVEUgVEFCTEUgQXdzRGF0YUNhdGFsb2cucmVwb3J0cy50YWJsZUlkMwogICAgICAgICAgICAgICAgV0lUSCAoCiAgICAgICAgICAgICAgICAgIGZvcm1hdCA9ICdQQVJRVUVUJwogICAgICAgICAgICAgICAgKSAKICAgICAgICAgICAgICAgIEFTICgKICAgICAgICAgIFdJVEggY29udGV4dF8gQVMgKAogICAgICBTRUxFQ1QgCiAgICAgICdhVXNlcicgQVMgdXNlcm5hbWUsIAogICAgICAnYUNhc2Vsb2FkJyBBUyBjYXNlbG9hZCwgCiAgICAgICdHRU5FUkFMJyBBUyBhY2NvdW50X3R5cGUgCiAgICAgIAogICAgICApLHByb21wdF8gQVMgKFNFTEVDVCAnJyApLGRhdGFzZXRfIEFTIChTRUxFQ1QgY291bnQoKikgKyAxIGFzIHRvdGFsX3BsdXNfb25lIGZyb20gdGFibGVJZDIpLHJlcG9ydF8gQVMgKFNFTEVDVCAqIEZST00gZGF0YXNldF8pLHBvbGljeV8gQVMgKFNFTEVDVCAqIEZST00gcmVwb3J0XyBXSEVSRSAxPTEpLGZpbHRlcl8gQVMgKFNFTEVDVCAqIEZST00gcG9saWN5XyBXSEVSRSAxPTEpClNFTEVDVCAqCiAgICAgICAgICBGUk9NIGZpbHRlcl8gT1JERVIgQlkgY29sdW1uX2EgYXNjCiAgICAgICAgICAgICAgICAp',
            0,
            SYSDATE
          )"""
    verify(jdbcTemplate).execute(firstMultiphaseInsert)
    verify(jdbcTemplate).execute(secondMultiphaseInsert)
    verify(jdbcTemplate).execute(thirdMultiphaseInsert)
    val inOrder = inOrder(jdbcTemplate)
    inOrder.verify(jdbcTemplate).execute(firstMultiphaseInsert)
    inOrder.verify(jdbcTemplate).execute(secondMultiphaseInsert.trimIndent())
    inOrder.verify(jdbcTemplate).execute(thirdMultiphaseInsert.trimIndent())
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
    assertEquals(StatementExecutionResponse(tableId3, executionId), actual)
  }

  @Test
  fun `executeQueryAsync should run a multiphase query once when there is exactly one multiphaseQuery defined`() {
    val database = "db"
    val catalog = "catalog"
    val startQueryExecutionRequest = setupBasicMocks(
      database = database,
      catalog = catalog,
      query = sqlStatement(tableId),
    )
    val datasource = Datasource("id", "name", database, catalog)
    val multiphaseQuery = listOf(
      MultiphaseQuery(0, datasource, dpdQuery),
    )
    whenever(dataset.multiphaseQuery).thenReturn(multiphaseQuery)
    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      tableId,
    )
    val actual = athenaApiRepository.executeQueryAsync(
      filters = emptyList(),
      sortColumn = "column_a",
      sortedAsc = true,
      policyEngineResult = TRUE_WHERE_CLAUSE,
      userToken = userToken,
      query = "",
      reportFilter = productDefinition.report.filter,
      datasource = productDefinition.datasource,
      reportSummaries = productDefinition.report.summary,
      allDatasets = productDefinition.allDatasets,
      productDefinitionId = productDefinition.id,
      productDefinitionName = productDefinition.name,
      reportOrDashboardId = productDefinition.report.id,
      reportOrDashboardName = productDefinition.report.name,
      multiphaseQueries = multiphaseQuery,
    )
    val firstMultiphaseInsert = """insert into 
          admin.multiphase_query_state (
          root_execution_id,
          current_execution_id,
          datasource_name,
          catalog,
          database,
          index,
          query,
          sequence_number,
          last_update
          )
          values (
            'someId',
            'someId',
            'name',
            'catalog',
            'db',
            0,
            'ICAgICAgICAgIC8qIGRwZElkIGRwZE5hbWUgcmVwb3J0SWQgcmVwb3J0TmFtZSAqLwogICAgICAgICAgQ1JFQVRFIFRBQkxFIEF3c0RhdGFDYXRhbG9nLnJlcG9ydHMuX2E2MjI3NDE3X2JkYWNfNDBiYl9iYzgxXzQ5Yzc1MGRhYWNkNyAKICAgICAgICAgIFdJVEggKAogICAgICAgICAgICBmb3JtYXQgPSAnUEFSUVVFVCcKICAgICAgICAgICkgCiAgICAgICAgICBBUyAoCiAgICAgICAgICBTRUxFQ1QgKiBGUk9NIFRBQkxFKHN5c3RlbS5xdWVyeShxdWVyeSA9PgogICAgICAgICAgICdXSVRIIGNvbnRleHRfIEFTICgKICAgICAgU0VMRUNUIAogICAgICAnJ2FVc2VyJycgQVMgdXNlcm5hbWUsIAogICAgICAnJ2FDYXNlbG9hZCcnIEFTIGNhc2Vsb2FkLCAKICAgICAgJydHRU5FUkFMJycgQVMgYWNjb3VudF90eXBlIAogICAgICBGUk9NIERVQUwKICAgICAgKSxwcm9tcHRfIEFTIChTRUxFQ1QgJycnJyBGUk9NIERVQUwpLGRhdGFzZXRfIEFTIChTRUxFQ1QgY29sdW1uX2EsY29sdW1uX2IgRlJPTSBzY2hlbWFfYS50YWJsZV9hKSxyZXBvcnRfIEFTIChTRUxFQ1QgKiBGUk9NIGRhdGFzZXRfKSxwb2xpY3lfIEFTIChTRUxFQ1QgKiBGUk9NIHJlcG9ydF8gV0hFUkUgMT0xKSxmaWx0ZXJfIEFTIChTRUxFQ1QgKiBGUk9NIHBvbGljeV8gV0hFUkUgMT0xKQpTRUxFQ1QgKgogICAgICAgICAgRlJPTSBmaWx0ZXJfIE9SREVSIEJZIGNvbHVtbl9hIGFzYycKICAgICAgICAgICApKSAKICAgICAgICAgICk7',
            0,
            SYSDATE
          )"""

    verify(jdbcTemplate).execute(firstMultiphaseInsert)
    verify(jdbcTemplate, times(1)).execute(any())
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
    verify(athenaClient, times(1)).startQueryExecution(any(StartQueryExecutionRequest::class.java))
    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
  }

  @Test
  fun `executeQueryAsync should throw an error when a subsequent multiphase query does not define a datasource connection`() {
    val database = "db"
    val catalog = "catalog"
    setupBasicMocks(
      database = database,
      catalog = catalog,
      query = multiphaseSqlNonLastQuery(),
    )
    val datasource1 = Datasource("id", "name", database, catalog)
    val datasource2 = Datasource("id2", "name2", database, catalog)
    val query2 = "SELECT count(*) as total from \${table[0]}"
    val multiphaseQuery = listOf(
      MultiphaseQuery(0, datasource1, dpdQuery),
      MultiphaseQuery(1, datasource2, query2),
    )
    val tableId2 = "tableId2"

    whenever(dataset.multiphaseQuery).thenReturn(multiphaseQuery)
    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      tableId,
      tableId2,
    )
    val exception = assertThrows(ValidationException::class.java) {
      athenaApiRepository.executeQueryAsync(
        filters = emptyList(),
        sortColumn = "column_a",
        sortedAsc = true,
        policyEngineResult = TRUE_WHERE_CLAUSE,
        userToken = userToken,
        query = "",
        reportFilter = productDefinition.report.filter,
        datasource = productDefinition.datasource,
        reportSummaries = productDefinition.report.summary,
        allDatasets = productDefinition.allDatasets,
        productDefinitionId = productDefinition.id,
        productDefinitionName = productDefinition.name,
        reportOrDashboardId = productDefinition.report.id,
        reportOrDashboardName = productDefinition.report.name,
        multiphaseQueries = multiphaseQuery,
      )
    }
    assertEquals(exception.message, "Query at index 1 has no connection defined in its datasource.")
  }

  private fun setupBasicMocks(
    whereClause: String? = TRUE_WHERE_CLAUSE,
    promptsCte: String? = emptyPromptsCte,
    datasetCte: String? = defaultDatasetCte,
    reportFilter: ReportFilter? = ReportFilter(name = REPORT_, query = DEFAULT_REPORT_CTE),
    database: String? = testDb,
    catalog: String? = testCatalog,
    cachedTableId: String? = tableId,
    query: String? = sqlStatement(
      tableId = cachedTableId!!,
      whereClauseCondition = whereClause,
      promptsCte = promptsCte,
      datasetCte = datasetCte,
      prefilter = reportFilter,
    ),
  ): StartQueryExecutionRequest {
    val queryExecutionContext = QueryExecutionContext.builder()
      .database(database)
      .catalog(catalog)
      .build()
    val startQueryExecutionRequest = StartQueryExecutionRequest.builder()
      .queryString(
        query,
      )
      .queryExecutionContext(queryExecutionContext)
      .workGroup(athenaWorkgroup)
      .build()
    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      cachedTableId,
    )
    whenever(productDefinition.id).thenReturn("dpdId")
    whenever(productDefinition.name).thenReturn("dpdName")
    whenever(productDefinition.reportDataset).thenReturn(dataset)
    whenever(productDefinition.datasource).thenReturn(datasource)
    whenever(productDefinition.report).thenReturn(report)
    whenever(productDefinition.report.id).thenReturn("reportId")
    whenever(productDefinition.report.name).thenReturn("reportName")
    whenever(productDefinition.report.filter).thenReturn(reportFilter)
    whenever(datasource.database).thenReturn(testDb)
    whenever(datasource.catalog).thenReturn(testCatalog)

    whenever(
      athenaClient.startQueryExecution(
        ArgumentMatchers.any(StartQueryExecutionRequest::class.java),
      ),
    ).thenReturn(startQueryExecutionResponse)

    whenever(
      startQueryExecutionResponse.queryExecutionId(),
    ).thenReturn(executionId)

    whenever(userToken.getUsername()).thenReturn(testUsername)
    whenever(userToken.getActiveCaseLoadId()).thenReturn(testCaseload)
    whenever(userToken.getCaseLoadIds()).thenReturn(listOf(testCaseload))

    return startQueryExecutionRequest
  }
}
