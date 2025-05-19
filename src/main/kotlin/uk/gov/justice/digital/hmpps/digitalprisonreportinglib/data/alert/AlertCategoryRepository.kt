package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.alert

import org.apache.commons.lang3.time.StopWatch
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse
import software.amazon.awssdk.services.athena.model.Row
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings.AthenaQueryHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator

@Service
@ConditionalOnBean(AthenaClient::class)
class AlertCategoryRepository(
  override val athenaClient: AthenaClient,
  override val tableIdGenerator: TableIdGenerator,
  @Value("\${dpr.lib.redshiftdataapi.athenaworkgroup:workgroupArn}")
  override val athenaWorkgroup: String,
  val athenaQueryHelper: AthenaQueryHelper = AthenaQueryHelper(),
) : AthenaApiRepository(athenaClient, tableIdGenerator, athenaWorkgroup) {

  companion object {
    const val NOMIS_CATALOG = "nomis"
    const val DIGITAL_PRISON_REPORTING_DB = "DIGITAL_PRISON_REPORTING"
    const val ALERT_CATEGORY_QUERY = "SELECT DOMAIN, CODE, DESCRIPTION FROM OMS_OWNER.REFERENCE_CODES WHERE DOMAIN='ALERT' OR DOMAIN IS NULL OR DOMAIN='ALERT_CODE' ORDER BY DOMAIN;"
  }

  fun executeStatementWaitAndGetResult(): List<AlertCategory> = try {
    val stopwatch = StopWatch.createStarted()
    val executionId = executeQueryAsync(
      datasource = Datasource("", "", DIGITAL_PRISON_REPORTING_DB, NOMIS_CATALOG),
      tableId = "notApplicableHere",
      query = ALERT_CATEGORY_QUERY,
    ).executionId
    athenaQueryHelper.waitForQueryToComplete(executionId, this::getStatementStatus)
    val results = fetchAllResults(executionId)
    stopwatch.stop()
    log.info("List of alert categories and codes retrieved successfully in ${stopwatch.time}.")
    results
  } catch (e: Exception) {
    log.error("Error retrieving list of alerts: ", e)
    emptyList()
  }

  private fun fetchAllResults(queryExecutionId: String): List<AlertCategory> {
    val getQueryResultsRequest: GetQueryResultsRequest =
      GetQueryResultsRequest.builder()
        .queryExecutionId(queryExecutionId)
        .build()
    var getQueryResultsResponse: GetQueryResultsResponse = athenaClient.getQueryResults(getQueryResultsRequest)
    val alertCategoryResultAcc: MutableList<AlertCategory> = mutableListOf()
    var page = 1
    while (true) {
      log.debug("Fetching list of establishments. Results page $page.")
      for ((v) in mapResults(getQueryResultsResponse, page)) {
        alertCategoryResultAcc.plus(v)
      }
      // If nextToken is null, there are no more pages to read. Break out of the loop.
      val nextToken = getQueryResultsResponse.nextToken() ?: break
      getQueryResultsResponse = athenaClient.getQueryResults(
        GetQueryResultsRequest
          .builder()
          .queryExecutionId(queryExecutionId)
          .nextToken(nextToken)
          .build(),
      )
      page++
    }
    return alertCategoryResultAcc
  }

  private fun mapResults(
    getQueryResultsResponse: GetQueryResultsResponse,
    page: Int,
  ): List<AlertCategory> = getQueryResultsResponse
    .resultSet()
    .rows()
    .mapIndexed { index, row ->
      // Process the row. The first row of the first page holds the column names.
      mapRow(page, index, row)
    }
    .filterNotNull()

  private fun mapRow(page: Int, index: Int, row: Row): AlertCategory? {
    if (page == 1 && index == 0) {
      // first row contains the table headers
      return null
    }
    return AlertCategory(
      row.data()[0].varCharValue(),
      row.data()[1].varCharValue(),
      row.data()[2].varCharValue(),
    )
  }
}
