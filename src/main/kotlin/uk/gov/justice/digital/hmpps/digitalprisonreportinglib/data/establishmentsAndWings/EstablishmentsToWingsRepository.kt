package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings

import org.apache.commons.lang3.time.StopWatch
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse
import software.amazon.awssdk.services.athena.model.Row
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.AthenaApiRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator

@Service
@ConditionalOnBean(AthenaClient::class)
class EstablishmentsToWingsRepository(
  override val athenaClient: AthenaClient,
  override val tableIdGenerator: TableIdGenerator,
  @Value("\${dpr.lib.redshiftdataapi.athenaworkgroup:workgroupArn}")
  override val athenaWorkgroup: String,
  val athenaQueryHelper: AthenaQueryHelper = AthenaQueryHelper(),
) : AthenaApiRepository(athenaClient, tableIdGenerator, athenaWorkgroup) {

  companion object {
    const val NOMIS_CATALOG = "nomis"
    const val DIGITAL_PRISON_REPORTING_DB = "DIGITAL_PRISON_REPORTING"
    const val ESTABLISHMENTS_TO_WINGS_QUERY = "SELECT DISTINCT LIVING_UNITS.agy_loc_id as establishment_code, AGENCY_LOCATIONS.description as establishment_name, LIVING_UNITS.AGY_LOC_ID || '-' || LIVING_UNITS.LEVEL_1_CODE as wing FROM OMS_OWNER.LIVING_UNITS JOIN OMS_OWNER.AGENCY_LOCATIONS ON LIVING_UNITS.agy_loc_id = AGENCY_LOCATIONS.agy_loc_id;"
  }
  fun executeStatementWaitAndGetResult(): MutableMap<String, List<EstablishmentToWing>> = try {
    val stopwatch = StopWatch.createStarted()
    val executionId = executeQueryAsync(
      datasource = Datasource("", "", DIGITAL_PRISON_REPORTING_DB, NOMIS_CATALOG),
      tableId = "notApplicableHere",
      query = ESTABLISHMENTS_TO_WINGS_QUERY,
    ).executionId
    athenaQueryHelper.waitForQueryToComplete(executionId, this::getStatementStatus)
    // waitForQueryToComplete(executionId)
    val results = fetchAllResults(executionId)
    stopwatch.stop()
    log.info("List of establishments and wings retrieved successfully in ${stopwatch.time}.")
    results
  } catch (e: Exception) {
    log.error("Error retrieving list of establishments and wings: ", e)
    mutableMapOf()
  }

  private fun fetchAllResults(queryExecutionId: String): MutableMap<String, List<EstablishmentToWing>> {
    val getQueryResultsRequest: GetQueryResultsRequest =
      GetQueryResultsRequest.builder()
        .queryExecutionId(queryExecutionId)
        .build()
    var getQueryResultsResponse: GetQueryResultsResponse = athenaClient.getQueryResults(getQueryResultsRequest)
    val establishmentToWingsAcc: MutableMap<String, List<EstablishmentToWing>> = mutableMapOf()
    var page = 1
    while (true) {
      log.debug("Fetching list of establishments. Results page $page.")
      for ((k, v) in groupWingsByEstablishment(getQueryResultsResponse, page)) {
        establishmentToWingsAcc[k] =
          establishmentToWingsAcc[k]?.plus(v) ?: v
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
    return establishmentToWingsAcc
  }

  private fun groupWingsByEstablishment(
    getQueryResultsResponse: GetQueryResultsResponse,
    page: Int,
  ): Map<String, List<EstablishmentToWing>> = getQueryResultsResponse
    .resultSet()
    .rows()
    .mapIndexed { index, row ->
      // Process the row. The first row of the first page holds the column names.
      mapRow(page, index, row)
    }
    .filterNotNull()
    .groupBy { it.establishmentCode }

  private fun mapRow(page: Int, index: Int, row: Row): EstablishmentToWing? {
    if (page == 1 && index == 0) {
      // first row contains the table headers
      return null
    }
    return EstablishmentToWing(
      row.data()[0].varCharValue(),
      row.data()[1].varCharValue(),
      row.data()[2].varCharValue(),
    )
  }
}
