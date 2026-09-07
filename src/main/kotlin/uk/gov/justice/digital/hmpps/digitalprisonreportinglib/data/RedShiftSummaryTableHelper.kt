package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
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

  fun buildSummaryQuery(query: String, tableId: String, summaryId: String, executionContext: ExecutionContext): String {
    val substitutedQuery = query.replace(TABLE_TOKEN_NAME, "reports.$tableId")
    val summaryTableId = tableIdGenerator.getTableSummaryId(tableId, summaryId)
    return """
      /* QUERY_INFO|||${executionContext.dataProductReportableInformation.id}|||${executionContext.dataProductReportableInformation.name}|||${executionContext.dataProductReportableInformation.datasource?.name ?: ""}|||${executionContext.dataProductReportableInformation.datasource?.database ?: ""}|||${executionContext.dataProductReportableInformation.datasource?.catalog ?: ""}|||${executionContext.dataProductReportableInformation.variantId}|||${executionContext.dataProductReportableInformation.variantName}|||${executionContext.hasProbationDatasources}|||SUMMARY|||END */
          CREATE EXTERNAL TABLE reports.$summaryTableId 
          STORED AS parquet 
          LOCATION 's3://$s3location/$summaryTableId/' 
          AS ($substitutedQuery);
    """
  }
}
