package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType

/**
 * A sink for the rows of a report download.
 *
 * Decouples the row producing pipeline (column selection, ordering, display names and
 * formulas, all in [CommonDataApiService.populateRowConsumer]) from the file format the
 * rows end up in, so the same pipeline can emit CSV or XLSX.
 */
interface ReportRowWriter : AutoCloseable {

  /**
   * Called once, before any call to [writeRow], with the columns in output order.
   */
  fun writeHeader(columns: List<ReportColumn>)

  /**
   * Values are in the same order as the columns passed to [writeHeader].
   */
  fun writeRow(values: List<Any?>)
}

/**
 * A column of a report download: its source field name, the heading shown to the user,
 * and the type declared for it in the dataset schema.
 */
data class ReportColumn(
  val name: String,
  val display: String,
  val type: ParameterType,
)
