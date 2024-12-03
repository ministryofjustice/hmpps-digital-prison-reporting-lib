package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings

import org.apache.commons.lang3.time.StopWatch
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.ColumnInfo
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse
import software.amazon.awssdk.services.athena.model.Row
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QUERY_ABORTED
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QUERY_FAILED
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QUERY_FINISHED
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator

@Service
class EstablishmentsAndWingsRepository(
  override val athenaClient: AthenaClient,
  override val tableIdGenerator: TableIdGenerator,
  @Value("\${dpr.lib.redshiftdataapi.athenaworkgroup:workgroupArn}")
  override val athenaWorkgroup: String,
) : AthenaApiRepository(athenaClient, tableIdGenerator, athenaWorkgroup) {

  companion object {
    const val ESTABLISHMENTS_TO_WINGS_QUERY = "SELECT DISTINCT LIVING_UNITS.agy_loc_id as establishment_code, AGENCY_LOCATIONS.description as establishment_name, LIVING_UNITS.level_1_code as wing FROM OMS_OWNER.LIVING_UNITS JOIN OMS_OWNER.AGENCY_LOCATIONS ON LIVING_UNITS.agy_loc_id = AGENCY_LOCATIONS.agy_loc_id;"
  }
  fun executeStatementWaitAndGetResult(): MutableMap<String, List<EstablishmentAndWing>> {
    return try {
      val stopwatch = StopWatch.createStarted()
      val executionId = executeQueryAsync(
        datasource = Datasource("", "", "DIGITAL_PRISON_REPORTING", "nomis"),
        tableId = "notApplicableHere",
        query = ESTABLISHMENTS_TO_WINGS_QUERY,
      ).executionId
      waitForQueryToComplete(executionId)
      val results = fetchAllResults(executionId)
      stopwatch.stop()
      log.info("List of establishments and wings retrieved successfully in ${stopwatch.duration.seconds} sec.")
      results
    } catch (e: Exception) {
      log.error("Error retrieving list of establishments and wings: ", e)
      mutableMapOf()
    }
  }

  private fun waitForQueryToComplete(executionId: String) {
    var isQueryStillRunning = true
    while (isQueryStillRunning) {
      val status = getStatementStatus(executionId)
      when (status.status) {
        QUERY_FAILED -> {
          throw RuntimeException(
            "Query Failed to run with Error Message: " +
              status.stateChangeReason,
          )
        }
        QUERY_ABORTED -> {
          throw RuntimeException("Query was cancelled.")
        }
        QUERY_FINISHED -> {
          isQueryStillRunning = false
        }
        else -> {
          Thread.sleep(500)
        }
      }
    }
  }

  private fun fetchAllResults(queryExecutionId: String): MutableMap<String, List<EstablishmentAndWing>> {
    val getQueryResultsRequest: GetQueryResultsRequest =
      GetQueryResultsRequest.builder()
        .queryExecutionId(queryExecutionId)
        .build()
    var getQueryResultsResponse: GetQueryResultsResponse = athenaClient.getQueryResults(getQueryResultsRequest)
    val establishmentToWings: MutableMap<String, List<EstablishmentAndWing>> = mutableMapOf()
    var page = 1
    while (true) {
      establishmentToWings.putAll(groupWingsByEstablishment(getQueryResultsResponse, page))
      // If nextToken is null, there are no more pages to read. Break out of the loop.
      if (getQueryResultsResponse.nextToken() == null) {
        break
      }
      getQueryResultsResponse = athenaClient.getQueryResults(
        GetQueryResultsRequest
          .builder()
          .queryExecutionId(queryExecutionId)
          .nextToken(getQueryResultsResponse.nextToken())
          .build(),
      )
      page++
    }
    return establishmentToWings
  }

  private fun groupWingsByEstablishment(
    getQueryResultsResponse: GetQueryResultsResponse,
    page: Int,
  ): Map<String, List<EstablishmentAndWing>> {
    return getQueryResultsResponse
      .resultSet()
      .rows()
      .mapIndexed { index, row ->
        // Process the row. The first row of the first page holds the column names.
        mapRow(page, index, row)
      }
      .filterNotNull()
      .groupBy { it.establishmentCode }
  }

  private fun mapRow(page: Int, index: Int, row: Row): EstablishmentAndWing? {
    if (page == 1 && index == 0) {
      // first row contains the table headers
      return null
    }
    return EstablishmentAndWing(
      row.data()[0].varCharValue(),
      row.data()[1].varCharValue(),
      row.data()[2].varCharValue(),
    )
  }
}
