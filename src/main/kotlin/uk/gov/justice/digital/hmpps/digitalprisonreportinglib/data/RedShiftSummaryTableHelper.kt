package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator

@Service
class RedShiftSummaryTableHelper(
  private val tableIdGenerator: TableIdGenerator,
  @Value("\${dpr.lib.redshiftdataapi.s3location:#{'dpr-working-development/reports'}}")
  private val s3location: String,
) {
  companion object {
    const val TABLE_TOKEN_NAME = "\${tableId}"
  }

  fun buildSummaryQuery(query: String, tableId: String, summaryId: String): String {
    val substitutedQuery = query.replace(TABLE_TOKEN_NAME, "reports.$tableId")
    val summaryTableId = tableIdGenerator.getTableSummaryId(tableId, summaryId)
    return """
          CREATE EXTERNAL TABLE reports.$summaryTableId 
          STORED AS parquet 
          LOCATION 's3://$s3location/$summaryTableId/' 
          AS ($substitutedQuery);
    """
  }
}