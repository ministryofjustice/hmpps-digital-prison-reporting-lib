package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.ColumnMetadata
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.Field
import software.amazon.awssdk.services.redshiftdata.model.GetStatementResultRequest
import software.amazon.awssdk.services.redshiftdata.model.GetStatementResultResponse
import software.amazon.awssdk.services.redshiftdata.model.SqlParameter
import software.amazon.awssdk.services.redshiftdata.model.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_END_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController.FiltersPrefix.RANGE_FILTER_START_SUFFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_DATASOURCE_NAME
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_POLICY_ENGINE_RESULT
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.Companion.REPOSITORY_TEST_QUERY
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.Companion.EXTERNAL_MOVEMENTS_PRODUCT_ID
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementExecutionStatus
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata.StatementResult
import java.time.LocalDateTime

class RedshiftDataApiRepositoryTest {

  companion object {
    val sqlStatement = """WITH dataset_ AS (SELECT prisoners.number AS prisonNumber,CONCAT(CONCAT(prisoners.lastname, ', '), substring(prisoners.firstname, 1, 1)) AS name,movements.time AS date,movements.direction,movements.type,movements.origin,movements.origin_code,movements.destination,movements.destination_code,movements.reason
FROM datamart.domain.movement_movement as movements
JOIN datamart.domain.prisoner_prisoner as prisoners
ON movements.prisoner = prisoners.id),policy_ AS (SELECT * FROM dataset_ WHERE (origin_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='out') OR (destination_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='in')),filter_ AS (SELECT * FROM policy_ WHERE lower(direction) = :direction)
SELECT *
          FROM filter_ ORDER BY date asc;
    """.trimMargin()

    val columnMetadata = listOf<ColumnMetadata>(
      ColumnMetadata.builder().name("id").typeName("varchar").build(),
      ColumnMetadata.builder().name("prisoner").typeName("int8").build(),
      ColumnMetadata.builder().name("date").typeName("timestamp").build(),
      ColumnMetadata.builder().name("time").typeName("timestamp").build(),
      ColumnMetadata.builder().name("direction").typeName("varchar").build(),
      ColumnMetadata.builder().name("type").typeName("varchar").build(),
      ColumnMetadata.builder().name("origin_code").typeName("varchar").build(),
      ColumnMetadata.builder().name("origin").typeName("varchar").build(),
      ColumnMetadata.builder().name("destination_code").typeName("varchar").build(),
      ColumnMetadata.builder().name("destination").typeName("varchar").build(),
      ColumnMetadata.builder().name("reason").typeName("varchar").build(),
    )
    val movementPrisoner1 = mapOf("id" to "171034.12", "prisoner" to 171034L, "date" to LocalDateTime.of(2010, 12, 17, 0, 0, 0), "time" to LocalDateTime.of(2010, 12, 17, 7, 12, 0), "direction" to "OUT", "type" to "CRT", "origin_code" to "LFI", "origin" to "LANCASTER FARMS (HMPYOI)", "destination_code" to "STHEMC", "destination" to "St. Helens Magistrates Court", "reason" to "Production (Sentence/Civil Custody)")
    val movementPrisoner1Fields = listOf<Field>(
      Field.builder().stringValue("171034.12").build(),
      Field.builder().longValue(171034).build(),
      Field.builder().stringValue("2010-12-17 00:00:00").build(),
      Field.builder().stringValue("2010-12-17 07:12:00").build(),
      Field.builder().stringValue("OUT").build(),
      Field.builder().stringValue("CRT").build(),
      Field.builder().stringValue("LFI").build(),
      Field.builder().stringValue("LANCASTER FARMS (HMPYOI)").build(),
      Field.builder().stringValue("STHEMC").build(),
      Field.builder().stringValue("St. Helens Magistrates Court").build(),
      Field.builder().stringValue("Production (Sentence/Civil Custody)").build(),
    )
    val movementPrisoner2 = mapOf("id" to "227482.1", "prisoner" to 227482L, "date" to LocalDateTime.of(2010, 12, 8, 0, 0, 0), "time" to LocalDateTime.of(2010, 12, 8, 10, 8, 0), "direction" to "IN", "type" to "ADM", "origin_code" to "IMM", "origin" to "Immigration", "destination_code" to "HRI", "destination" to "Haslar Immigration Removal Centre", "reason" to "Detained Immigration Act 71 -Wait Deport")
    val movementPrisoner2Fields = listOf<Field>(
      Field.builder().stringValue("227482.1").build(),
      Field.builder().longValue(227482).build(),
      Field.builder().stringValue("2010-12-08 00:00:00").build(),
      Field.builder().stringValue("2010-12-08 10:08:00").build(),
      Field.builder().stringValue("IN").build(),
      Field.builder().stringValue("ADM").build(),
      Field.builder().stringValue("IMM").build(),
      Field.builder().stringValue("Immigration").build(),
      Field.builder().stringValue("HRI").build(),
      Field.builder().stringValue("Haslar Immigration Removal Centre").build(),
      Field.builder().stringValue("Detained Immigration Act 71 -Wait Deport").build(),
    )
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
  fun `executeQueryAsync should replace filters which contain a dot in their name with an underscore and not error`() {
    val dateStartKeyUnderscore = "date_start"
    val dateEndKeyUnderscore = "date_end"
    val startDate = "2024-02-16"
    val endDate = "2024-02-17"
    val startDateFilter = ConfiguredApiRepository.Filter("date", startDate, FilterType.DATE_RANGE_START)
    val endDateFilter = ConfiguredApiRepository.Filter("date", endDate, FilterType.DATE_RANGE_END)
    val sqlStatement =
      """WITH dataset_ AS (SELECT prisoners.number AS prisonNumber,CONCAT(CONCAT(prisoners.lastname, ', '), substring(prisoners.firstname, 1, 1)) AS name,movements.time AS date,movements.direction,movements.type,movements.origin,movements.origin_code,movements.destination,movements.destination_code,movements.reason
FROM datamart.domain.movement_movement as movements
JOIN datamart.domain.prisoner_prisoner as prisoners
ON movements.prisoner = prisoners.id),policy_ AS (SELECT * FROM dataset_ WHERE (origin_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='out') OR (destination_code IN ('HEI','LWSTMC','NSI','LCI','TCI') AND lower(direction)='in')),filter_ AS (SELECT * FROM policy_ WHERE date >= CAST(:$dateStartKeyUnderscore AS timestamp) AND date < (CAST(:$dateEndKeyUnderscore AS timestamp) + INTERVAL '1' day))
SELECT *
          FROM filter_ ORDER BY date asc;
      """.trimIndent()
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
    ).thenReturn("someId")

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

  @Test
  fun `getStatementResult should call the Redshift Data API and return the existing results`() {
    val redshiftDataClient = mock<RedshiftDataClient>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(redshiftDataClient, mock())
    val statementId = "statementId"
    val resultStatementResponse = GetStatementResultResponse.builder()
      .columnMetadata(columnMetadata)
      .records(listOf(movementPrisoner1Fields, movementPrisoner2Fields))
      .build()

    whenever(
      redshiftDataClient.getStatementResult(
        GetStatementResultRequest.builder()
          .id(statementId)
          .build(),
      ),
    ).thenReturn(resultStatementResponse)

    val expected = StatementResult(listOf<Map<String, Any?>>(movementPrisoner1, movementPrisoner2))
    val actual = redshiftDataApiRepository.getStatementResult(statementId)

    assertEquals(expected, actual)
  }

  @Test
  fun `getStatementResult should call the Redshift Data API with a request containing a nextToken when a nextToken exists`() {
    val redshiftDataClient = mock<RedshiftDataClient>()
    val redshiftDataApiRepository = RedshiftDataApiRepository(redshiftDataClient, mock())
    val statementId = "statementId"
    val nextTokenRequest = "batch1"
    val nextTokenResponse = "batch2"
    val resultStatementResponse = GetStatementResultResponse.builder()
      .columnMetadata(columnMetadata)
      .records(listOf(movementPrisoner1Fields, movementPrisoner2Fields))
      .nextToken(nextTokenResponse)
      .build()

    whenever(
      redshiftDataClient.getStatementResult(
        GetStatementResultRequest.builder()
          .id(statementId)
          .nextToken(nextTokenRequest)
          .build(),
      ),
    ).thenReturn(resultStatementResponse)

    val expected = StatementResult(listOf<Map<String, Any?>>(movementPrisoner1, movementPrisoner2), nextTokenResponse)
    val actual = redshiftDataApiRepository.getStatementResult(statementId, nextTokenRequest)

    assertEquals(expected, actual)
  }
}
