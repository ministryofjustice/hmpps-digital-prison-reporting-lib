package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import java.util.Base64
import java.util.UUID

@Service
class TableIdGenerator {

  fun generateNewExternalTableId(): String = "_" + UUID.randomUUID().toString().replace("-", "_")

  fun getTableSummaryId(tableId: String, summaryId: String): String = "${tableId}_${summaryId.replace('-', '_')}"

  fun generateScheduledDatasetId(definition: SingleReportProductDefinition): String {
    val id = "${definition.id}:${definition.reportDataset.id}"
    val encodedId = Base64.getEncoder().encodeToString(id.toByteArray())
    val updatedId = encodedId.replace("=", "_")
    return "_$updatedId"
  }
}
