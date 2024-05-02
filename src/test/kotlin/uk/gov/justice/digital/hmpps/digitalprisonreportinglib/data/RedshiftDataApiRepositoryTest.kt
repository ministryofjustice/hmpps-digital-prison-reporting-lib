package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.SqlParameter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_DATASOURCE_NAME
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_POLICY_ENGINE_RESULT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_QUERY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.EXTERNAL_MOVEMENTS_PRODUCT_ID

class RedshiftDataApiRepositoryTest {

  @Test
  fun `should call the redshift data api with the correct query and return the execution id`() {
    val redshiftDataClient = mock<RedshiftDataClient>()
    val executeStatementRequestBuilder = mock<ExecuteStatementRequest.Builder>()
    val executeStatementResponse = mock<ExecuteStatementResponse>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(redshiftDataClient, executeStatementRequestBuilder)

    val sqlStatement = """WITH dataset_ AS (SELECT prisoners.number AS prisonNumber,CONCAT(CONCAT(prisoners.lastname, ', '), substring(prisoners.firstname, 1, 1)) AS name,movements.time AS date,movements.direction,movements.type,movements.origin,movements.origin_code,movements.destination,movements.destination_code,movements.reason
FROM datamart.domain.movement_movement as movements
JOIN datamart.domain.prisoner_prisoner as prisoners
ON movements.prisoner = prisoners.id),policy_ AS (SELECT * FROM dataset_ WHERE (origin_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='out') OR (destination_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='in')),filter_ AS (SELECT * FROM policy_ WHERE lower(direction) = :direction)
SELECT *
          FROM filter_ ORDER BY date asc;
    """.trimMargin()
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
}
