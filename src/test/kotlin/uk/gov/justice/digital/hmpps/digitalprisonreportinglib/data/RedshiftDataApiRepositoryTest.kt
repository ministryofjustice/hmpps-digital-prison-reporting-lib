package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_POLICY_ENGINE_RESULT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_QUERY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.FALSE_WHERE_CLAUSE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.TRUE_WHERE_CLAUSE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy.PolicyResult
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
        ON movements.prisoner = prisoners.id),policy_ AS (SELECT * FROM dataset_ WHERE (origin_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='out') OR (destination_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='in')),filter_ AS (SELECT * FROM policy_ WHERE lower(direction) = 'out')
        SELECT *
                  FROM filter_ ORDER BY date asc
                  );
      """.trimIndent()

    val movementPrisoner1 = mapOf("id" to "171034.12", "prisoner" to 171034L, "date" to LocalDateTime.of(2010, 12, 17, 0, 0, 0), "time" to LocalDateTime.of(2010, 12, 17, 7, 12, 0), "direction" to "OUT", "type" to "CRT", "origin_code" to "LFI", "origin" to "LANCASTER FARMS (HMPYOI)", "destination_code" to "STHEMC", "destination" to "St. Helens Magistrates Court", "reason" to "Production (Sentence/Civil Custody)")
    val movementPrisoner2 = mapOf("id" to "227482.1", "prisoner" to 227482L, "date" to LocalDateTime.of(2010, 12, 8, 0, 0, 0), "time" to LocalDateTime.of(2010, 12, 8, 10, 8, 0), "direction" to "IN", "type" to "ADM", "origin_code" to "IMM", "origin" to "Immigration", "destination_code" to "HRI", "destination" to "Haslar Immigration Removal Centre", "reason" to "Detained Immigration Act 71 -Wait Deport")
    private const val REDSHIFT_DATA_API_DB: String = "redshiftDB"
    private const val REDSHIFT_DATA_API_CLUSTER_ID: String = "redshiftClusterId"
    private const val REDSHIFT_DATA_API_SECRET_ARN: String = "redshiftSecretArn"
  }

  @Test
  fun `executeQueryAsync should call the redshift data api with the correct query and return the execution id and table id`() {
    val redshiftDataClient = mock<RedshiftDataClient>()
    val executeStatementResponse = mock<ExecuteStatementResponse>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val tableId = "_a6227417_bdac_40bb_bc81_49c750daacd7"
    val executionId = "someId"
    val redshiftDataApiRepository = RedshiftDataApiRepository(
      redshiftDataClient,
      tableIdGenerator,
      REDSHIFT_DATA_API_DB,
      REDSHIFT_DATA_API_CLUSTER_ID,
      REDSHIFT_DATA_API_SECRET_ARN,
    )
    val executeStatementRequest = ExecuteStatementRequest.builder()
      .clusterIdentifier(REDSHIFT_DATA_API_CLUSTER_ID)
      .database(REDSHIFT_DATA_API_DB)
      .secretArn(REDSHIFT_DATA_API_SECRET_ARN)
      .sql(sqlStatement(tableId))
      .build()

    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      tableId,
    )

    whenever(
      redshiftDataClient.executeStatement(
        executeStatementRequest,
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
      policyEngineResult = REPOSITORY_TEST_POLICY_ENGINE_RESULT,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
  }

  @Test
  fun `executeQueryAsync should call the redshift data api with the correct query including all filters and return the execution id and table id`() {
    val startDate = "2024-02-16"
    val endDate = "2024-02-17"
    val directionFilter = ConfiguredApiRepository.Filter("direction", "out")
    val startDateFilter = ConfiguredApiRepository.Filter("date", startDate, FilterType.DATE_RANGE_START)
    val endDateFilter = ConfiguredApiRepository.Filter("date", endDate, FilterType.DATE_RANGE_END)
    val nameDynamicFilter = ConfiguredApiRepository.Filter("name", "LastNa", FilterType.DYNAMIC)
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
ON movements.prisoner = prisoners.id),policy_ AS (SELECT * FROM dataset_ WHERE (origin_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='out') OR (destination_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='in')),filter_ AS (SELECT * FROM policy_ WHERE date >= CAST('$startDate' AS timestamp) AND date < (CAST('$endDate' AS timestamp) + INTERVAL '1' day) AND lower(direction) = 'out' AND name ILIKE 'LastNa%')
SELECT *
          FROM filter_ ORDER BY date asc
          );
      """.trimIndent()
    val redshiftDataClient = mock<RedshiftDataClient>()
    val executeStatementResponse = mock<ExecuteStatementResponse>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(
      redshiftDataClient,
      tableIdGenerator,
      REDSHIFT_DATA_API_DB,
      REDSHIFT_DATA_API_CLUSTER_ID,
      REDSHIFT_DATA_API_SECRET_ARN,
    )
    val executeStatementRequest = ExecuteStatementRequest.builder()
      .clusterIdentifier(REDSHIFT_DATA_API_CLUSTER_ID)
      .database(REDSHIFT_DATA_API_DB)
      .secretArn(REDSHIFT_DATA_API_SECRET_ARN)
      .sql(sqlStatement)
      .build()

    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      tableId,
    )

    whenever(
      redshiftDataClient.executeStatement(
        executeStatementRequest,
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
        directionFilter,
        nameDynamicFilter,
      ),
      sortColumn = "date",
      sortedAsc = true,
      policyEngineResult = REPOSITORY_TEST_POLICY_ENGINE_RESULT,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
  }

  @Test
  fun `getStatementStatus should call the redshift data api with the correct statement ID and return the StatementExecutionStatus`() {
    val redshiftDataClient = mock<RedshiftDataClient>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(
      redshiftDataClient,
      tableIdGenerator,
      REDSHIFT_DATA_API_DB,
      REDSHIFT_DATA_API_CLUSTER_ID,
      REDSHIFT_DATA_API_SECRET_ARN,
    )
    val statementId = "statementId"
    val status = "FINISHED"
    val duration = 278109264L
    val query = "SELECT * FROM datamart.domain.movement_movement limit 10;"
    val resultRows = 0L
    val resultSize = 0L
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
    val executeStatementResponse = mock<ExecuteStatementResponse>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(
      redshiftDataClient,
      tableIdGenerator,
      REDSHIFT_DATA_API_DB,
      REDSHIFT_DATA_API_CLUSTER_ID,
      REDSHIFT_DATA_API_SECRET_ARN,
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
        ON movements.prisoner = prisoners.id),policy_ AS (SELECT * FROM dataset_ WHERE (origin_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='out') OR (destination_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='in')),filter_ AS (SELECT * FROM policy_ WHERE $TRUE_WHERE_CLAUSE)
        SELECT *
                  FROM filter_ ORDER BY date asc
                  );
      """.trimIndent()

    val executeStatementRequest = ExecuteStatementRequest.builder()
      .clusterIdentifier(REDSHIFT_DATA_API_CLUSTER_ID)
      .database(REDSHIFT_DATA_API_DB)
      .secretArn(REDSHIFT_DATA_API_SECRET_ARN)
      .sql(finalQuery)
      .build()

    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      tableId,
    )

    whenever(
      redshiftDataClient.executeStatement(
        executeStatementRequest,
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
      policyEngineResult = REPOSITORY_TEST_POLICY_ENGINE_RESULT,
    )

    assertEquals(StatementExecutionResponse(tableId, executionId), actual)
  }

  @ParameterizedTest
  @CsvSource(
    "${PolicyResult.POLICY_PERMIT}, $TRUE_WHERE_CLAUSE",
    "${PolicyResult.POLICY_DENY}, $FALSE_WHERE_CLAUSE",
  )
  fun `executeQueryAsync should call the redshift data api with the converted sql where clause for permit and denied policies`(policyEngineResult: String, queryWhereClause: String) {
    val redshiftDataClient = mock<RedshiftDataClient>()
    val executeStatementResponse = mock<ExecuteStatementResponse>()
    val tableIdGenerator = mock<TableIdGenerator>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(
      redshiftDataClient,
      tableIdGenerator,
      REDSHIFT_DATA_API_DB,
      REDSHIFT_DATA_API_CLUSTER_ID,
      REDSHIFT_DATA_API_SECRET_ARN,
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
        ON movements.prisoner = prisoners.id),policy_ AS (SELECT * FROM dataset_ WHERE $queryWhereClause),filter_ AS (SELECT * FROM policy_ WHERE $TRUE_WHERE_CLAUSE)
        SELECT *
                  FROM filter_ ORDER BY date asc
                  );
      """.trimIndent()

    val executeStatementRequest = ExecuteStatementRequest.builder()
      .clusterIdentifier(REDSHIFT_DATA_API_CLUSTER_ID)
      .database(REDSHIFT_DATA_API_DB)
      .secretArn(REDSHIFT_DATA_API_SECRET_ARN)
      .sql(finalQuery)
      .build()

    whenever(
      tableIdGenerator.generateNewExternalTableId(),
    ).thenReturn(
      tableId,
    )

    whenever(
      redshiftDataClient.executeStatement(
        executeStatementRequest,
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
      policyEngineResult = policyEngineResult,
    )

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
      tableIdGenerator,
      REDSHIFT_DATA_API_DB,
      REDSHIFT_DATA_API_CLUSTER_ID,
      REDSHIFT_DATA_API_SECRET_ARN,
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
  fun `count should make a JDBC call and return the existing results`() {
    val tableId = "tableId"
    val jdbcTemplate = mock<NamedParameterJdbcTemplate>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(
      mock(),
      mock(),
      REDSHIFT_DATA_API_DB,
      REDSHIFT_DATA_API_CLUSTER_ID,
      REDSHIFT_DATA_API_SECRET_ARN,
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
