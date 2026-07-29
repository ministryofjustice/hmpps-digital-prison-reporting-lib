package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import java.io.Writer

/**
 * Writes report rows as CSV.
 *
 * Values are quoted only when they contain a delimiter, a quote or a newline, which keeps
 * the output identical to what this library has always produced. Note that quoting does
 * not stop Excel type-guessing a value such as a room number `1.5.2` into a date when the
 * file is opened by double-clicking it — quotes delimit fields, they do not declare types.
 * Use [XlsxRowWriter] where that matters.
 */
class CsvRowWriter(private val writer: Writer) : ReportRowWriter {

  override fun writeHeader(columns: List<ReportColumn>) {
    writer.write(columns.joinToString(",") { escapeCsv(it.display) })
    writer.write("\n")
  }

  override fun writeRow(values: List<Any?>) {
    values.forEachIndexed { index, value ->
      if (index > 0) writer.write(",")
      writer.write(escapeCsv(value))
    }
    writer.write("\n")
  }

  override fun close() {
    writer.flush()
  }

  private fun escapeCsv(value: Any?): String {
    if (value == null) return ""

    val str = value.toString()
    val needsEscaping = str.contains(",") || str.contains("\"") || str.contains("\n")

    return if (needsEscaping) {
      "\"${str.replace("\"", "\"\"")}\""
    } else {
      str
    }
  }
}
