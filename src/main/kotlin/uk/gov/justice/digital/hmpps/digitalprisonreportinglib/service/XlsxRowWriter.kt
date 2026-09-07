package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.apache.poi.ss.SpreadsheetVersion
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import java.io.OutputStream
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * Writes report rows as an XLSX workbook.
 *
 * The reason this format exists: in a CSV every value is untyped text and Excel guesses a
 * type for it on open, so a room number such as `1.5.2` is silently turned into a date and
 * `007` into `7`. In XLSX each cell carries its type explicitly, so anything written as a
 * string stays a string.
 *
 * Dates and numbers declared as such in the dataset schema are still written as real dates
 * and numbers, so sorting and filtering behave as users expect. Everything else is written
 * as text.
 *
 * Uses POI's streaming [SXSSFWorkbook] so memory stays bounded on large reports; rows
 * beyond the access window are spilled to temp files, which [close] cleans up.
 */
class XlsxRowWriter(
  private val out: OutputStream,
  private val sheetName: String = "Report",
) : ReportRowWriter {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    /** Rows POI keeps in memory before spilling to disk. */
    private const val ROW_ACCESS_WINDOW_SIZE = 100

    /** Excel refuses to open a workbook with a cell string longer than this. */
    private const val MAX_CELL_TEXT_LENGTH = 32_767

    /**
     * Excel stores numbers as IEEE-754 doubles, so integers beyond this lose precision.
     * Anything larger is written as text rather than silently corrupted.
     */
    private const val MAX_EXACT_INTEGER = 9_007_199_254_740_992L

    private const val DATE_FORMAT = "dd/MM/yyyy"
    private const val DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss"
  }

  private val workbook = SXSSFWorkbook(ROW_ACCESS_WINDOW_SIZE)

  // Styles are created once per workbook: POI caps a workbook at 64k styles, so creating
  // them per row would fail part way through a large report.
  private val dateStyle: CellStyle = workbook.createCellStyle().apply {
    dataFormat = workbook.creationHelper.createDataFormat().getFormat(DATE_FORMAT)
  }
  private val dateTimeStyle: CellStyle = workbook.createCellStyle().apply {
    dataFormat = workbook.creationHelper.createDataFormat().getFormat(DATE_TIME_FORMAT)
  }
  private val headerStyle: CellStyle = workbook.createCellStyle().apply {
    setFont(workbook.createFont().apply { bold = true })
  }

  private val maxRowsPerSheet = SpreadsheetVersion.EXCEL2007.maxRows

  private lateinit var columns: List<ReportColumn>
  private lateinit var sheet: Sheet
  private var sheetCount = 0
  private var rowIndexInSheet = 0

  override fun writeHeader(columns: List<ReportColumn>) {
    this.columns = columns
    newSheet()
  }

  override fun writeRow(values: List<Any?>) {
    // Excel caps a sheet at ~1,048,576 rows, so a larger report rolls onto another sheet
    // with the header repeated rather than being silently truncated.
    if (rowIndexInSheet >= maxRowsPerSheet) {
      newSheet()
    }

    val row = sheet.createRow(rowIndexInSheet++)
    values.forEachIndexed { index, value ->
      writeCell(row.createCell(index), value, columns.getOrNull(index)?.type)
    }
  }

  override fun close() {
    try {
      // A report with no rows never reaches writeHeader, and POI cannot write a workbook
      // with no sheets, so give it an empty one.
      if (sheetCount == 0) {
        workbook.createSheet(sheetName)
      }
      workbook.write(out)
      out.flush()
    } finally {
      // Since POI 5.3.0 close() also disposes of the temp files SXSSF spilled rows to,
      // so there is no separate dispose() call to make. Without this every download leaks.
      workbook.close()
    }
  }

  private fun newSheet() {
    sheetCount++
    sheet = workbook.createSheet(if (sheetCount == 1) sheetName else "$sheetName ($sheetCount)")
    rowIndexInSheet = 0

    val header = sheet.createRow(rowIndexInSheet++)
    columns.forEachIndexed { index, column ->
      header.createCell(index).apply {
        setCellValue(truncate(column.display))
        cellStyle = headerStyle
      }
    }
    sheet.createFreezePane(0, 1)
  }

  private fun writeCell(cell: Cell, value: Any?, type: ParameterType?) {
    if (value == null) return

    when (type) {
      ParameterType.Date -> writeDateCell(cell, value, dateStyle)
      ParameterType.DateTime, ParameterType.Timestamp -> writeDateCell(cell, value, dateTimeStyle)
      ParameterType.Integer, ParameterType.Long -> writeIntegerCell(cell, value)
      ParameterType.Double, ParameterType.Float -> writeDecimalCell(cell, value)
      ParameterType.Boolean -> writeBooleanCell(cell, value)
      // Strings, times and anything unrecognised stay text. This is the case that fixes
      // room numbers, reference codes and identifiers with leading zeros.
      else -> cell.setCellValue(truncate(value.toString()))
    }
  }

  private fun writeDateCell(cell: Cell, value: Any?, style: CellStyle) {
    val temporal = toLocalDateTime(value)
    if (temporal == null) {
      // A formula may have already rendered this into a display string, and some
      // datasources hand back values that do not match the declared type. Either way,
      // text is the safe answer.
      cell.setCellValue(truncate(value.toString()))
      return
    }
    cell.setCellValue(temporal)
    cell.cellStyle = style
  }

  private fun writeIntegerCell(cell: Cell, value: Any?) {
    val number = when (value) {
      is Number -> value.toLong()
      else -> value.toString().trim().toLongOrNull()
    }
    if (number == null || number >= MAX_EXACT_INTEGER || number <= -MAX_EXACT_INTEGER) {
      cell.setCellValue(truncate(value.toString()))
      return
    }
    cell.setCellValue(number.toDouble())
  }

  private fun writeDecimalCell(cell: Cell, value: Any?) {
    val number = when (value) {
      is Number -> value.toDouble()
      else -> value.toString().trim().toDoubleOrNull()
    }
    if (number == null || number.isNaN() || number.isInfinite()) {
      cell.setCellValue(truncate(value.toString()))
      return
    }
    cell.setCellValue(number)
  }

  private fun writeBooleanCell(cell: Cell, value: Any?) {
    when (value) {
      is Boolean -> cell.setCellValue(value)
      else -> when (value.toString().trim().lowercase()) {
        "true" -> cell.setCellValue(true)
        "false" -> cell.setCellValue(false)
        else -> cell.setCellValue(truncate(value.toString()))
      }
    }
  }

  private fun toLocalDateTime(value: Any?): LocalDateTime? = when (value) {
    is LocalDateTime -> value
    is LocalDate -> value.atStartOfDay()
    is Timestamp -> value.toLocalDateTime()
    is java.sql.Date -> value.toLocalDate().atStartOfDay()
    is java.util.Date -> LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault())
    is OffsetDateTime -> value.toLocalDateTime()
    is ZonedDateTime -> value.toLocalDateTime()
    else -> parseTemporal(value.toString())
  }

  private fun parseTemporal(value: String): LocalDateTime? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    return try {
      LocalDateTime.parse(trimmed)
    } catch (_: DateTimeParseException) {
      try {
        LocalDate.parse(trimmed).atStartOfDay()
      } catch (_: DateTimeParseException) {
        null
      }
    }
  }

  private fun truncate(value: String): String = if (value.length <= MAX_CELL_TEXT_LENGTH) {
    value
  } else {
    log.warn("Truncating a cell value of ${value.length} characters to the Excel limit of $MAX_CELL_TEXT_LENGTH.")
    value.take(MAX_CELL_TEXT_LENGTH)
  }
}
