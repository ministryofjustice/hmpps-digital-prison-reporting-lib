package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TableIdGenerator {

  fun generateNewExternalTableId(): String = "_" + UUID.randomUUID().toString().replace("-", "_")

  fun getTableSummaryId(tableId: String, summaryId: String): String = "${tableId}_${summaryId.replace('-', '_')}"
}
