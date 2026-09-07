package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

class XlsxRowWriterTest {

  private fun column(name: String, type: ParameterType) = ReportColumn(name, "The $name", type)

  private fun write(columns: List<ReportColumn>, rows: List<List<Any?>>): Sheet {
    val out = ByteArrayOutputStream()
    XlsxRowWriter(out).use { writer ->
      writer.writeHeader(columns)
      rows.forEach(writer::writeRow)
    }
    return XSSFWorkbook(ByteArrayInputStream(out.toByteArray())).getSheetAt(0)
  }

  @Test
  fun `values that Excel would misread as dates or numbers stay text`() {
    // The bug this format exists to fix: opening the csv equivalent of this row in Excel
    // turns 1.5.2 into a date, drops the leading zeros from 007 and expands 1E5.
    val sheet = write(
      columns = listOf(
        column("room", ParameterType.String),
        column("reference", ParameterType.String),
        column("code", ParameterType.String),
        column("received", ParameterType.String),
      ),
      rows = listOf(listOf("1.5.2", "007", "1E5", "1/2/2024")),
    )

    val row = sheet.getRow(1)
    assertThat(row.map { it.cellType }).containsOnly(CellType.STRING)
    assertThat(row.map { it.stringCellValue })
      .containsExactly("1.5.2", "007", "1E5", "1/2/2024")
  }

  @Test
  fun `the header row uses the column display names`() {
    val sheet = write(
      columns = listOf(column("room", ParameterType.String), column("wing", ParameterType.String)),
      rows = listOf(listOf("1.5.2", "A")),
    )

    assertThat(sheet.getRow(0).map { it.stringCellValue }).containsExactly("The room", "The wing")
  }

  @Test
  fun `columns declared as dates are written as real dates so sorting still works`() {
    val sheet = write(
      columns = listOf(
        column("date", ParameterType.Date),
        column("datetime", ParameterType.DateTime),
        column("timestamp", ParameterType.Timestamp),
      ),
      rows = listOf(
        listOf(
          LocalDate.of(2024, 2, 1),
          LocalDateTime.of(2024, 2, 1, 13, 30),
          Timestamp.valueOf(LocalDateTime.of(2024, 2, 1, 13, 30)),
        ),
      ),
    )

    val row = sheet.getRow(1)
    assertThat(row.map { it.cellType }).containsOnly(CellType.NUMERIC)
    assertThat(row.map { DateUtil.isCellDateFormatted(it) }).containsOnly(true)
    assertThat(row.getCell(0).localDateTimeCellValue).isEqualTo(LocalDateTime.of(2024, 2, 1, 0, 0))
    assertThat(row.getCell(1).localDateTimeCellValue).isEqualTo(LocalDateTime.of(2024, 2, 1, 13, 30))
    assertThat(row.getCell(2).localDateTimeCellValue).isEqualTo(LocalDateTime.of(2024, 2, 1, 13, 30))
  }

  @Test
  fun `a date column whose value is not a date falls back to text`() {
    // Formulas can render a date column into a display string before it reaches the writer.
    val sheet = write(
      columns = listOf(column("released", ParameterType.Date)),
      rows = listOf(listOf("Not yet released")),
    )

    val cell = sheet.getRow(1).getCell(0)
    assertThat(cell.cellType).isEqualTo(CellType.STRING)
    assertThat(cell.stringCellValue).isEqualTo("Not yet released")
  }

  @Test
  fun `numeric columns are written as numbers`() {
    val sheet = write(
      columns = listOf(
        column("count", ParameterType.Long),
        column("age", ParameterType.Integer),
        column("rate", ParameterType.Double),
      ),
      rows = listOf(listOf(42L, "17", 1.5)),
    )

    val row = sheet.getRow(1)
    assertThat(row.map { it.cellType }).containsOnly(CellType.NUMERIC)
    assertThat(row.getCell(0).numericCellValue).isEqualTo(42.0)
    assertThat(row.getCell(1).numericCellValue).isEqualTo(17.0)
    assertThat(row.getCell(2).numericCellValue).isEqualTo(1.5)
  }

  @Test
  fun `integers too large for Excel to hold exactly stay text rather than lose precision`() {
    // Excel stores numbers as doubles, so anything past 2^53 would be silently rounded.
    val sheet = write(
      columns = listOf(column("id", ParameterType.Long)),
      rows = listOf(listOf(9_007_199_254_740_993L)),
    )

    val cell = sheet.getRow(1).getCell(0)
    assertThat(cell.cellType).isEqualTo(CellType.STRING)
    assertThat(cell.stringCellValue).isEqualTo("9007199254740993")
  }

  @Test
  fun `booleans are written as booleans and unparseable ones as text`() {
    val sheet = write(
      columns = listOf(
        column("active", ParameterType.Boolean),
        column("known", ParameterType.Boolean),
      ),
      rows = listOf(listOf(true, "unknown")),
    )

    assertThat(sheet.getRow(1).getCell(0).booleanCellValue).isTrue()
    assertThat(sheet.getRow(1).getCell(1).stringCellValue).isEqualTo("unknown")
  }

  @Test
  fun `nulls are written as blank cells`() {
    val sheet = write(
      columns = listOf(column("a", ParameterType.String), column("b", ParameterType.String)),
      rows = listOf(listOf(null, "set")),
    )

    assertThat(sheet.getRow(1).getCell(0).cellType).isEqualTo(CellType.BLANK)
    assertThat(sheet.getRow(1).getCell(1).stringCellValue).isEqualTo("set")
  }

  @Test
  fun `values containing commas and quotes need no escaping`() {
    // Unlike csv, the xlsx container carries these verbatim.
    val sheet = write(
      columns = listOf(column("notes", ParameterType.String)),
      rows = listOf(listOf("""Smith, John said "hello"""")),
    )

    assertThat(sheet.getRow(1).getCell(0).stringCellValue).isEqualTo("""Smith, John said "hello"""")
  }

  @Test
  fun `cell text longer than the Excel limit is truncated`() {
    val sheet = write(
      columns = listOf(column("notes", ParameterType.String)),
      rows = listOf(listOf("x".repeat(40_000))),
    )

    assertThat(sheet.getRow(1).getCell(0).stringCellValue).hasSize(32_767)
  }

  @Test
  fun `a report with no rows still produces a readable workbook`() {
    val out = ByteArrayOutputStream()
    XlsxRowWriter(out).use { }

    val workbook = XSSFWorkbook(ByteArrayInputStream(out.toByteArray()))
    assertThat(workbook.numberOfSheets).isEqualTo(1)
    assertThat(workbook.getSheetAt(0).physicalNumberOfRows).isZero()
  }
}
