package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.SqlParameter
import software.amazon.awssdk.services.redshiftdata.model.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_DATASOURCE_NAME
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_POLICY_ENGINE_RESULT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_QUERY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.EXTERNAL_MOVEMENTS_PRODUCT_ID
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.StatementExecutionStatus

class RedshiftDataApiRepositoryTest {

  companion object {
    val sqlStatement = """WITH dataset_ AS (SELECT prisoners.number AS prisonNumber,CONCAT(CONCAT(prisoners.lastname, ', '), substring(prisoners.firstname, 1, 1)) AS name,movements.time AS date,movements.direction,movements.type,movements.origin,movements.origin_code,movements.destination,movements.destination_code,movements.reason
FROM datamart.domain.movement_movement as movements
JOIN datamart.domain.prisoner_prisoner as prisoners
ON movements.prisoner = prisoners.id),policy_ AS (SELECT * FROM dataset_ WHERE (origin_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='out') OR (destination_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='in')),filter_ AS (SELECT * FROM policy_ WHERE lower(direction) = :direction)
SELECT *
          FROM filter_ ORDER BY date asc;
    """.trimMargin()
  }

  @Test
  fun `executeQueryAsync should call the redshift data api with the correct query and return the execution id`() {
    val redshiftDataClient = mock<RedshiftDataClient>()
    val executeStatementRequestBuilder = mock<ExecuteStatementRequest.Builder>()
    val executeStatementResponse = mock<ExecuteStatementResponse>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(redshiftDataClient, executeStatementRequestBuilder)

    val mockedBuilderWithSql = ExecuteStatementRequest.builder()
      .clusterIdentifier("ab")
      .database("cd")
      .secretArn("ef")
      .sql(sqlStatement)
    whenever(
      executeStatementRequestBuilder.sql(
        sqlStatement,
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
    ).thenReturn("someId")

    val actual = redshiftDataApiRepository.executeQueryAsync(
      query = REPOSITORY_TEST_QUERY,
      filters = listOf(ConfiguredApiRepository.Filter("direction", "out")),
      sortColumn = "date",
      sortedAsc = true,
      reportId = EXTERNAL_MOVEMENTS_PRODUCT_ID,
      policyEngineResult = REPOSITORY_TEST_POLICY_ENGINE_RESULT,
      dataSourceName = REPOSITORY_TEST_DATASOURCE_NAME,
    )

    assertEquals("someId", actual)
  }

  @Test
  fun `getStatementStatus should call the redshift data api with the correct statement ID and return the StatementExecutionStatus`() {
    val redshiftDataClient = mock<RedshiftDataClient>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(redshiftDataClient, mock())
    val statementId = "statementId"
    val status = "FINISHED"
    val duration = 278109264L
    val query = "SELECT * FROM datamart.domain.movement_movement limit 10;"
    val resultRows = 10L
    val executeStatementResponse = DescribeStatementResponse.builder()
      .status(status)
      .duration(duration)
      .queryString(query)
      .resultRows(resultRows)
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
    )
    val actual = redshiftDataApiRepository.getStatementStatus(statementId)

    assertEquals(expected, actual)
  }

  @Test
  fun `executeQueryAsync should call the redshift data api and not error when no filters are provided`() {
    val redshiftDataClient = mock<RedshiftDataClient>()
    val executeStatementRequestBuilder = mock<ExecuteStatementRequest.Builder>()
    val executeStatementResponse = mock<ExecuteStatementResponse>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(redshiftDataClient, executeStatementRequestBuilder)
    val finalQuery =
      """WITH dataset_ AS (SELECT prisoners.number AS prisonNumber,CONCAT(CONCAT(prisoners.lastname, ', '), substring(prisoners.firstname, 1, 1)) AS name,movements.time AS date,movements.direction,movements.type,movements.origin,movements.origin_code,movements.destination,movements.destination_code,movements.reason
FROM datamart.domain.movement_movement as movements
JOIN datamart.domain.prisoner_prisoner as prisoners
ON movements.prisoner = prisoners.id),policy_ AS (SELECT * FROM dataset_ WHERE (origin_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='out') OR (destination_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='in')),filter_ AS (SELECT * FROM policy_ WHERE TRUE)
SELECT *
          FROM filter_ ORDER BY date asc;
      """.trimIndent()

    val mockedBuilderWithSql = ExecuteStatementRequest.builder()
      .clusterIdentifier("ab")
      .database("cd")
      .secretArn("ef")
      .sql(finalQuery)

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
    ).thenReturn("someId")

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

    assertEquals("someId", actual)
  }
}
