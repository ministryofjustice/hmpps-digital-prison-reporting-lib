package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.SqlParameter
import software.amazon.awssdk.services.redshiftdata.model.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_DATASOURCE_NAME
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_POLICY_ENGINE_RESULT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_QUERY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.EXTERNAL_MOVEMENTS_PRODUCT_ID
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator
import java.time.LocalDateTime

class RedshiftDataApiRepositoryTest {

  companion object {
    fun sqlStatement(tableId: String) =
      """
                  CREATE EXTERNAL TABLE reports.$tableId 
                  STORED AS parquet 
                  LOCATION 's3://dpr-working-development/reports/$tableId/' 
                  AS ( 
                  WITH dataset_ AS (SELECT prisoners.number AS prisonNumber,CONCAT(CONCAT(prisoners.lastname, ', '), substring(prisoners.firstname, 1, 1)) AS name,movements.time AS date,movements.direction,movements.type,movements.origin,movements.origin_code,movements.destination,movements.destination_code,movements.reason
        FROM datamart.domain.movement_movement as movements
        JOIN datamart.domain.prisoner_prisoner as prisoners
        ON movements.prisoner = prisoners.id),policy_ AS (SELECT * FROM dataset_ WHERE (origin_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='out') OR (destination_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='in')),filter_ AS (SELECT * FROM policy_ WHERE lower(direction) = :direction)
        SELECT *
                  FROM filter_ ORDER BY date asc
                  );
      """.trimIndent()

    val movementPrisoner1 = mapOf("id" to "171034.12", "prisoner" to 171034L, "date" to LocalDateTime.of(2010, 12, 17, 0, 0, 0), "time" to LocalDateTime.of(2010, 12, 17, 7, 12, 0), "direction" to "OUT", "type" to "CRT", "origin_code" to "LFI", "origin" to "LANCASTER FARMS (HMPYOI)", "destination_code" to "STHEMC", "destination" to "St. Helens Magistrates Court", "reason" to "Production (Sentence/Civil Custody)")
    val movementPrisoner2 = mapOf("id" to "227482.1", "prisoner" to 227482L, "date" to LocalDateTime.of(2010, 12, 8, 0, 0, 0), "time" to LocalDateTime.of(2010, 12, 8, 10, 8, 0), "direction" to "IN", "type" to "ADM", "origin_code" to "IMM", "origin" to "Immigration", "destination_code" to "HRI", "destination" to "Haslar Immigration Removal Centre", "reason" to "Detained Immigration Act 71 -Wait Deport")
  }

  @Test
  fun `executeQueryAsync should call the redshift data api with the correct query and return the execution id`() {
    val redshiftDataClient = mock<RedshiftDataClient>()
    val executeStatementRequestBuilder = mock<ExecuteStatementRequest.Builder>()
    val executeStatementResponse = mock<ExecuteStatementResponse>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val tableId = "_a6227417_bdac_40bb_bc81_49c750daacd7"
    val executionId = "someId"
    val redshiftDataApiRepository = RedshiftDataApiRepository(
      redshiftDataClient,
      executeStatementRequestBuilder,
      tableIdGenerator,
    )

    val mockedBuilderWithSql = ExecuteStatementRequest.builder()
      .clusterIdentifier("ab")
      .database("cd")
      .secretArn("ef")
      .sql(sqlStatement(tableId))

    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      tableId,
    )

    whenever(
      executeStatementRequestBuilder.sql(
        sqlStatement(tableId),
      ),
    ).thenReturn(
      mockedBuilderWithSql,
    )

    val queryParams = listOf(SqlParameter.builder().name("direction").value("out").build())
    whenever(
      executeStatementRequestBuilder.parameters(queryParams),
    ).thenReturn(
      mockedBuilderWithSql
        .parameters(queryParams),
    )

    whenever(
      redshiftDataClient.executeStatement(
        mockedBuilderWithSql.build(),
      ),
    ).thenReturn(executeStatementResponse)

    whenever(
      executeStatementResponse.id(),
    ).thenReturn(executionId)

    val actual = redshiftDataApiRepository.executeQueryAsync(
      query = REPOSITORY_TEST_QUERY,
      filters = listOf(ConfiguredApiRepository.Filter("direction", "out")),
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = REPOSITORY_TEST_POLICY_ENGINE_RESULT,
      dataSourceName = REPOSITORY_TEST_DATASOURCE_NAME,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
  }

  @Test
  fun `executeQueryAsync should replace filters which contain a dot in their name with an underscore and not error`() {
    val dateStartKeyUnderscore = "date_start"
    val dateEndKeyUnderscore = "date_end"
    val startDate = "2024-02-16"
    val endDate = "2024-02-17"
    val startDateFilter = ConfiguredApiRepository.Filter("date", startDate, FilterType.DATE_RANGE_START)
    val endDateFilter = ConfiguredApiRepository.Filter("date", endDate, FilterType.DATE_RANGE_END)
    val tableId = "_a6227417_bdac_40bb_bc81_49c750daacd7"
    val executionId = "someId"
    val sqlStatement =
      """          CREATE EXTERNAL TABLE reports.$tableId 
          STORED AS parquet 
          LOCATION 's3://dpr-working-development/reports/$tableId/' 
          AS ( 
          WITH dataset_ AS (SELECT prisoners.number AS prisonNumber,CONCAT(CONCAT(prisoners.lastname, ', '), substring(prisoners.firstname, 1, 1)) AS name,movements.time AS date,movements.direction,movements.type,movements.origin,movements.origin_code,movements.destination,movements.destination_code,movements.reason
FROM datamart.domain.movement_movement as movements
JOIN datamart.domain.prisoner_prisoner as prisoners
ON movements.prisoner = prisoners.id),policy_ AS (SELECT * FROM dataset_ WHERE (origin_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='out') OR (destination_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='in')),filter_ AS (SELECT * FROM policy_ WHERE date >= CAST(:$dateStartKeyUnderscore AS timestamp) AND date < (CAST(:$dateEndKeyUnderscore AS timestamp) + INTERVAL '1' day))
SELECT *
          FROM filter_ ORDER BY date asc
          );
      """.trimIndent()
    val redshiftDataClient = mock<RedshiftDataClient>()
    val executeStatementRequestBuilder = mock<ExecuteStatementRequest.Builder>()
    val executeStatementResponse = mock<ExecuteStatementResponse>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(
      redshiftDataClient,
      executeStatementRequestBuilder,
      tableIdGenerator,
    )

    val mockedBuilderWithSql = ExecuteStatementRequest.builder()
      .clusterIdentifier("ab")
      .database("cd")
      .secretArn("ef")
      .sql(sqlStatement)

    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      tableId,
    )

    whenever(
      executeStatementRequestBuilder.sql(
        sqlStatement,
      ),
    ).thenReturn(
      mockedBuilderWithSql,
    )

    val queryParamsWithUnderscores = listOf(
      SqlParameter.builder().name(dateStartKeyUnderscore).value(startDate).build(),
      SqlParameter.builder().name(dateEndKeyUnderscore).value(endDate).build(),
    )
    whenever(
      executeStatementRequestBuilder.parameters(queryParamsWithUnderscores),
    ).thenReturn(
      mockedBuilderWithSql
        .parameters(queryParamsWithUnderscores),
    )

    val queryParamsWithDots = listOf(
      SqlParameter.builder().name("date$RANGE_FILTER_START_SUFFIX").value(startDate).build(),
      SqlParameter.builder().name("date$RANGE_FILTER_END_SUFFIX").value(endDate).build(),
    )
    whenever(
      executeStatementRequestBuilder.parameters(queryParamsWithDots),
    ).thenThrow(ValidationException::class.java)

    whenever(
      redshiftDataClient.executeStatement(
        mockedBuilderWithSql.build(),
      ),
    ).thenReturn(executeStatementResponse)

    whenever(
      executeStatementResponse.id(),
    ).thenReturn(executionId)

    val actual = redshiftDataApiRepository.executeQueryAsync(
      query = REPOSITORY_TEST_QUERY,
      filters = listOf(
        startDateFilter,
        endDateFilter,
      ),
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = REPOSITORY_TEST_POLICY_ENGINE_RESULT,
      dataSourceName = REPOSITORY_TEST_DATASOURCE_NAME,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
  }

  @Test
  fun `getStatementStatus should call the redshift data api with the correct statement ID and return the StatementExecutionStatus`() {
    val redshiftDataClient = mock<RedshiftDataClient>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(
      redshiftDataClient,
      mock(),
      tableIdGenerator,
    )
    val statementId = "statementId"
    val status = "FINISHED"
    val duration = 278109264L
    val query = "SELECT * FROM datamart.domain.movement_movement limit 10;"
    val resultRows = 10L
    val resultSize = 100L
    val executeStatementResponse = DescribeStatementResponse.builder()
      .status(status)
      .duration(duration)
      .queryString(query)
      .resultRows(resultRows)
      .resultSize(resultSize)
      .build()

    whenever(
      redshiftDataClient.describeStatement(
        DescribeStatementRequest.builder()
          .id(statementId)
          .build(),
      ),
    ).thenReturn(executeStatementResponse)

    val expected = StatementExecutionStatus(
      status,
      duration,
      query,
      resultRows,
      resultSize,
    )
    val actual = redshiftDataApiRepository.getStatementStatus(statementId)

    assertEquals(expected, actual)
  }

  @Test
  fun `executeQueryAsync should call the redshift data api and not error when no filters are provided`() {
    val redshiftDataClient = mock<RedshiftDataClient>()
    val executeStatementRequestBuilder = mock<ExecuteStatementRequest.Builder>()
    val executeStatementResponse = mock<ExecuteStatementResponse>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(
      redshiftDataClient,
      executeStatementRequestBuilder,
      tableIdGenerator,
    )
    val tableId = "_a6227417_bdac_40bb_bc81_49c750daacd7"
    val executionId = "someId"
    val finalQuery =
      """
                  CREATE EXTERNAL TABLE reports.$tableId 
                  STORED AS parquet 
                  LOCATION 's3://dpr-working-development/reports/$tableId/' 
                  AS ( 
                  WITH dataset_ AS (SELECT prisoners.number AS prisonNumber,CONCAT(CONCAT(prisoners.lastname, ', '), substring(prisoners.firstname, 1, 1)) AS name,movements.time AS date,movements.direction,movements.type,movements.origin,movements.origin_code,movements.destination,movements.destination_code,movements.reason
        FROM datamart.domain.movement_movement as movements
        JOIN datamart.domain.prisoner_prisoner as prisoners
        ON movements.prisoner = prisoners.id),policy_ AS (SELECT * FROM dataset_ WHERE (origin_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='out') OR (destination_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='in')),filter_ AS (SELECT * FROM policy_ WHERE TRUE)
        SELECT *
                  FROM filter_ ORDER BY date asc
                  );
      """.trimIndent()

    val mockedBuilderWithSql = ExecuteStatementRequest.builder()
      .clusterIdentifier("ab")
      .database("cd")
      .secretArn("ef")
      .sql(finalQuery)

    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      tableId,
    )

    whenever(
      executeStatementRequestBuilder.sql(
        finalQuery,
      ),
    ).thenReturn(
      mockedBuilderWithSql,
    )

    val queryParams = emptyList<SqlParameter>()
    whenever(
      executeStatementRequestBuilder.parameters(queryParams),
    ).thenThrow(ValidationException::class.java)

    whenever(
      redshiftDataClient.executeStatement(
        mockedBuilderWithSql.build(),
      ),
    ).thenReturn(executeStatementResponse)

    whenever(
      executeStatementResponse.id(),
    ).thenReturn(executionId)

    val actual = redshiftDataApiRepository.executeQueryAsync(
      query = REPOSITORY_TEST_QUERY,
      filters = emptyList(),
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = REPOSITORY_TEST_POLICY_ENGINE_RESULT,
      dataSourceName = REPOSITORY_TEST_DATASOURCE_NAME,
    )

    verify(executeStatementRequestBuilder, times(0)).parameters(any<List<SqlParameter>>())

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
  }

  @Test
  fun `getStatementResult should make a paginated JDBC call and return the existing results`() {
    val redshiftDataClient = mock<RedshiftDataClient>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val tableId = "tableId"
    val jdbcTemplate = mock<NamedParameterJdbcTemplate>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(
      redshiftDataClient,
      mock(),
      tableIdGenerator,
    )
    val selectedPage = 1L
    val pageSize = 10L
    val expected = listOf<Map<String, Any?>>(movementPrisoner1, movementPrisoner2)

    whenever(
      jdbcTemplate.queryForList(
        eq("SELECT * FROM reports.$tableId limit $pageSize OFFSET ($selectedPage - 1) * $pageSize;"),
        any<MapSqlParameterSource>(),
      ),
    ).thenReturn(expected)

    val actual = redshiftDataApiRepository.getPaginatedExternalTableResult(tableId, selectedPage, pageSize, jdbcTemplate)

    assertEquals(expected, actual)
  }

  @Test
  fun `count should make a paginated JDBC call and return the existing results`() {
    val tableId = "tableId"
    val jdbcTemplate = mock<NamedParameterJdbcTemplate>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(
      mock(),
      mock(),
      mock(),
    )
    val expected = listOf<Map<String, Any?>>(mapOf("total" to 5L))

    whenever(
      jdbcTemplate.queryForList(
        eq("SELECT COUNT(1) as total FROM reports.$tableId;"),
        any<MapSqlParameterSource>(),
      ),
    ).thenReturn(expected)

    val actual = redshiftDataApiRepository.count(tableId, jdbcTemplate)

    assertEquals(5L, actual)
  }
}
