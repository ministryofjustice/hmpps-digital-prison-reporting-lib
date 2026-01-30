package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovementOriginCaseloadDirectionIn
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Visible
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.util.*

class FormulaEngineTest {

  companion object {
    private const val NAME = "name"
    private const val DATE = "date"
    private const val MONEY = "money"
    private const val DESTINATION = "destination"
    private const val DESTINATION_CODE = "destination_code"
    private const val PRISON_NUMBER = "prison_number"
  }

  @Test
  fun `Formula engine should interpolate vars correctly`() {
    val row: Map<String, Any> = mapOf(
      NAME to "LastName6, F",
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:destination_code",
        display = "Destination Code",
        visible = Visible.TRUE,
        formula = "\${destination}",
      ),
    )
    val expectedRow: Map<String, Any> = mapOf(
      NAME to "LastName6, F",
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "Manchester",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine can interpolate multiple fields`() {
    val row: Map<String, Any> = mapOf(
      NAME to "LastName6, F",
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:destination_code",
        display = "Destination Code",
        visible = Visible.TRUE,
        formula = "\${destination}:\${destination_code}:\${name}",
      ),
      ReportField(
        name = "\$ref:destination",
        display = "Destination",
        visible = Visible.TRUE,
      ),
      ReportField(
        name = "\$ref:name",
        display = "Name",
        visible = Visible.TRUE,
      ),
      ReportField(
        name = "\$ref:date",
        display = "Date",
        visible = Visible.TRUE,
      ),
    )
    val expectedRow: Map<String, Any> = mapOf(
      NAME to "LastName6, F",
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "Manchester:MNCH:LastName6, F",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine can interpolate fields as long as they are part of the row`() {
    val row: Map<String, Any> = mapOf(
      NAME to "LastName6, F",
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:destination_code",
        display = "Destination Code",
        visible = Visible.TRUE,
        formula = "\${destination}:\${destination_code}:\${name}",
      ),
    )
    val expectedRow: Map<String, Any> = mapOf(
      NAME to "LastName6, F",
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "Manchester:MNCH:LastName6, F",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine can interpolate fields as long as they are part of the row even if they are not part of the report fields`() {
    val row: Map<String, Any> = mapOf(
      NAME to "LastName6, F",
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:destination_code",
        display = "Destination Code",
        visible = Visible.TRUE,
        formula = "\${destination}:\${name}",
      ),
    )
    val expectedRow: Map<String, Any> = mapOf(
      NAME to "LastName6, F",
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "Manchester:LastName6, F",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine ignores fields with empty string formulas and without any formulas`() {
    val row: Map<String, Any> = mapOf(
      NAME to "LastName6, F",
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:destination_code",
        display = "Destination Code",
        visible = Visible.TRUE,
        formula = "",
      ),
      ReportField(
        name = "\$ref:destination",
        display = "Destination",
        visible = Visible.TRUE,
      ),
      ReportField(
        name = "\$ref:name",
        display = "Name",
        visible = Visible.TRUE,
      ),
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(row, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine does not apply any formulas given an empty list of report fields`() {
    val row: Map<String, Any> = mapOf(
      NAME to "LastName6, F",
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(emptyList())
    assertEquals(row, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine outputs the raw formula as the field value for invalid formulas`() {
    val row: Map<String, Any> = mapOf(
      NAME to "LastName6, F",
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:destination_code",
        display = "Destination Code",
        visible = Visible.TRUE,
        formula = "\${non_existing}",
      ),
    )
    val expectedRow: Map<String, Any> = mapOf(
      NAME to "LastName6, F",
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "\${non_existing}",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine outputs empty String for null fields with formulas`() {
    val row: Map<String, Any?> = mapOf(
      NAME to null,
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:destination_code",
        display = "Destination Code",
        visible = Visible.TRUE,
        formula = "\${name}",
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to null,
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine outputs correct html for make_url formula when new tab is true`() {
    val makeUrlFormula = "make_url('https://prisoner-\${env}.digital.prison.service.justice.gov.uk/prisoner/\${prison_number}',\${name},TRUE)"
    val prisonNumber = "ABC123"
    val name = "LastName6, F"
    val row: Map<String, Any> = mapOf(
      NAME to name,
      PRISON_NUMBER to prisonNumber,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:destination_code",
        display = "Destination Code",
        visible = Visible.TRUE,
        formula = "\${destination}:\${destination_code}:\${name}",
      ),
      ReportField(
        name = "\$ref:destination",
        display = "Destination",
        visible = Visible.TRUE,
      ),
      ReportField(
        name = "\$ref:name",
        display = "Name",
        visible = Visible.TRUE,
      ),
      ReportField(
        name = "\$ref:prison_number",
        display = "Prison Number",
        visible = Visible.TRUE,
        formula = makeUrlFormula,

      ),
    )
    val expectedRow: Map<String, Any> = mapOf(
      NAME to name,
      PRISON_NUMBER to "<a href=\'https://prisoner-test.digital.prison.service.justice.gov.uk/prisoner/${prisonNumber}\' target=\"_blank\">$name</a>",
      DESTINATION to "Manchester",
      DESTINATION_CODE to "Manchester:MNCH:LastName6, F",
    )
    val formulaEngine = FormulaEngine(reportFields, "test")
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine outputs correct html for make_url formula when new tab is false`() {
    val makeUrlFormula = "make_url('https://prisoner-\${env}.digital.prison.service.justice.gov.uk/prisoner/\${prison_number}',\${name},FALSE)"
    val prisonNumber = "ABC123"
    val name = "LastName6, F"
    val row: Map<String, Any> = mapOf(
      NAME to name,
      PRISON_NUMBER to prisonNumber,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:destination",
        display = "Destination",
        visible = Visible.TRUE,
        formula = makeUrlFormula,
      ),
    )
    val expectedRow: Map<String, Any> = mapOf(
      NAME to name,
      PRISON_NUMBER to prisonNumber,
      DESTINATION to "<a href=\'https://prisoner-dev.digital.prison.service.justice.gov.uk/prisoner/${prisonNumber}\' >$name</a>",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(reportFields, "dev")
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine outputs correct html for make_url formula when there is no env`() {
    val makeUrlFormula = "make_url('https://prisoner-\${env}.digital.prison.service.justice.gov.uk/prisoner/\${prison_number}',\${name},FALSE)"
    val prisonNumber = "ABC123"
    val name = "LastName6, F"
    val row: Map<String, Any> = mapOf(
      NAME to name,
      PRISON_NUMBER to prisonNumber,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:destination",
        display = "Destination",
        visible = Visible.TRUE,
        formula = makeUrlFormula,
      ),
    )
    val expectedRow: Map<String, Any> = mapOf(
      NAME to name,
      PRISON_NUMBER to prisonNumber,
      DESTINATION to "<a href=\'https://prisoner.digital.prison.service.justice.gov.uk/prisoner/${prisonNumber}\' >$name</a>",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "dev, dev.",
      "test, test.",
      "null,''",
    ],
    nullValues = ["null"],
  )
  fun `Formula engine outputs correct html for make_url formula when the env is at the start of the host url`(environment: String?, result: String?) {
    val makeUrlFormula = "make_url('https://\${env}.moic.service.justice.gov.uk/prisons/\${prison_caseload}/prisoners/\${nomis_offender_id}/allocation/history',\${nomis_offender_id},TRUE)"
    val prisonCaseload = "DEF"
    val nomisOffenderId = "ABC123"
    val prisonCaseloadName = "prison_caseload"
    val nomisOffenderIdName = "nomis_offender_id"
    val row: Map<String, Any> = mapOf(
      prisonCaseloadName to prisonCaseload,
      nomisOffenderIdName to nomisOffenderId,
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:nomis_offender_id",
        display = "NOMS ID",
        visible = Visible.TRUE,
        formula = makeUrlFormula,
      ),
    )
    val expectedRow: Map<String, Any> = mapOf(
      prisonCaseloadName to prisonCaseload,
      nomisOffenderIdName to "<a href=\'https://${result}moic.service.justice.gov.uk/prisons/$prisonCaseload/prisoners/$nomisOffenderId/allocation/history' target=\"_blank\">$nomisOffenderId</a>",
    )
    val formulaEngine = FormulaEngine(reportFields, environment)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine outputs the html for make_url formula with empty strings in place of null row values`() {
    val makeUrlFormula = "make_url('https://prisoner-\${env}.digital.prison.service.justice.gov.uk/prisoner/\${prison_number}',\${name},FALSE)"
    val name = "LastName6, F"
    val row: Map<String, Any?> = mapOf(
      NAME to name,
      PRISON_NUMBER to null,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:destination",
        display = "Destination",
        visible = Visible.TRUE,
        formula = makeUrlFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to name,
      PRISON_NUMBER to null,
      DESTINATION to "<a href=\'https://prisoner.digital.prison.service.justice.gov.uk/prisoner/\' >$name</a>",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @ParameterizedTest
  @CsvSource(
    "dd/MM/yyyy, 01/06/2023",
    "dd/MM/yyyy hh:mm, 01/06/2023 12:00",
  )
  fun `Formula engine formats the datetime based on the provided format in the format_date formula`(dateFormat: String, expectedDate: String) {
    val formatDateFormula = "format_date(\${date}, '$dateFormat')"
    val name = "LastName6, F"
    val row: Map<String, Any?> = mapOf(
      NAME to name,
      DATE to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:date",
        display = "Date",
        visible = Visible.TRUE,
        formula = formatDateFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to name,
      DATE to expectedDate,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @ParameterizedTest
  @CsvSource(
    "dd/MM/yyyy, 01/06/2023",
    "dd/MM/yyyy hh:mm, 01/06/2023 12:00",
  )
  fun `Formula engine formats the datetime based on the provided format in the format_date formula regardless of the date column name`(dateFormat: String, expectedDate: String) {
    val formatDateFormula = "format_date(\${date1}, '$dateFormat')"
    val name = "LastName6, F"
    val row: Map<String, Any?> = mapOf(
      NAME to name,
      "date1" to externalMovementOriginCaseloadDirectionIn.time,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:date1",
        display = "Date",
        visible = Visible.TRUE,
        formula = formatDateFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to name,
      "date1" to expectedDate,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine formats the date based on the provided format in the format_date formula`() {
    val formatDateFormula = "format_date(\${date}, 'dd/MM/yyyy')"
    val name = "LastName6, F"
    val row: Map<String, Any?> = mapOf(
      NAME to name,
      DATE to LocalDate.of(2023, 6, 1),
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:date",
        display = "Date",
        visible = Visible.TRUE,
        formula = formatDateFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to name,
      DATE to "01/06/2023",
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine returns an empty string for a null date and the format_date formula does not error`() {
    val formatDateFormula = "format_date(\${date}, 'dd/MM/yyyy')"
    val name = "LastName6, F"
    val row: Map<String, Any?> = mapOf(
      NAME to name,
      DATE to null,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:date",
        display = "Date",
        visible = Visible.TRUE,
        formula = formatDateFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to name,
      DATE to "",
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine throws an error for an unrecognised date type`() {
    val formatDateFormula = "format_date(\${date}, 'dd/MM/yyyy')"
    val name = "LastName6, F"
    val row: Map<String, Any?> = mapOf(
      NAME to name,
      DATE to "I'm not a date",
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:date",
        display = "Date",
        visible = Visible.TRUE,
        formula = formatDateFormula,
      ),
    )
    val formulaEngine = FormulaEngine(reportFields)

    val exception = assertThrows<IllegalArgumentException> { formulaEngine.applyFormulas(row) }

    assertThat(exception.message)
      .startsWith("Could not parse date:")
      .endsWith(", of type class kotlin.String")
  }

  @Test
  fun `Formula engine accepts java Date`() {
    val formatDateFormula = "format_date(\${date}, 'dd/MM/yyyy')"
    val name = "LastName6, F"
    val row: Map<String, Any?> = mapOf(
      NAME to name,
      DATE to Date(0),
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:date",
        display = "Date",
        visible = Visible.TRUE,
        formula = formatDateFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to name,
      DATE to "01/01/1970",
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine accepts SQL Date`() {
    val formatDateFormula = "format_date(\${date}, 'dd/MM/yyyy')"
    val name = "LastName6, F"
    val row: Map<String, Any?> = mapOf(
      NAME to name,
      DATE to java.sql.Date(0),
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:date",
        display = "Date",
        visible = Visible.TRUE,
        formula = formatDateFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to name,
      DATE to "01/01/1970",
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine accepts various different numerical inputs and formulae, and formats them correctly`() {
    val formulae = arrayOf("#,##0.00", "#,###", "##.##")
    val inputs = arrayOf<Number>(123, 123.00, 123.53, 1231.01)
    testFormatNumber(formulae[0], inputs[0], "123.00")
    testFormatNumber(formulae[1], inputs[0], "123")
    testFormatNumber(formulae[2], inputs[0], "123")

    testFormatNumber(formulae[0], inputs[1], "123.00")
    testFormatNumber(formulae[1], inputs[1], "123")
    testFormatNumber(formulae[2], inputs[1], "123")

    testFormatNumber(formulae[0], inputs[2], "123.53")
    testFormatNumber(formulae[1], inputs[2], "124")
    testFormatNumber(formulae[2], inputs[2], "123.53")

    testFormatNumber(formulae[0], inputs[3], "1,231.01")
    testFormatNumber(formulae[1], inputs[3], "1,231")
    testFormatNumber(formulae[2], inputs[3], "1231.01")
  }

  @Test
  fun `formula engine uses the default value provided to the default_value formula if the first parameter is null or empty`() {
    val defaultValueFormula = "default_value(\${prison_number},'-')"
    val name = "LastName6, F"
    val row: Map<String, Any?> = mapOf(
      NAME to name,
      PRISON_NUMBER to null,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:$PRISON_NUMBER",
        display = PRISON_NUMBER,
        visible = Visible.TRUE,
        formula = defaultValueFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to name,
      PRISON_NUMBER to "-",
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `formula engine uses the first parameter value provided to the default_value formula when this is not null or empty`() {
    val defaultValueFormula = "default_value(\${prison_number},'-')"
    val name = "LastName6, F"
    val row: Map<String, Any?> = mapOf(
      NAME to name,
      PRISON_NUMBER to "A123",
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:$PRISON_NUMBER",
        display = PRISON_NUMBER,
        visible = Visible.TRUE,
        formula = defaultValueFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to name,
      PRISON_NUMBER to "A123",
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `formula engine makes all characters lowercase when lower function is called`() {
    val lowerCaseFormula = "lower(\${name})"
    val row: Map<String, Any?> = mapOf(
      NAME to "LastName6, F",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:$NAME",
        display = NAME,
        visible = Visible.TRUE,
        formula = lowerCaseFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to "lastname6, f",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `formula engine makes all characters uppercase when upper function is called`() {
    val lowerCaseFormula = "upper(\${name})"
    val row: Map<String, Any?> = mapOf(
      NAME to "LastName6, F",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:$NAME",
        display = NAME,
        visible = Visible.TRUE,
        formula = lowerCaseFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to "LASTNAME6, F",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `formula engine makes first character of every word uppercase and the rest lowercase when wordcap function is called`() {
    val lowerCaseFormula = "wordcap(\${name})"
    val row: Map<String, Any?> = mapOf(
      NAME to "JoHN D, sMiTh",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:$NAME",
        display = NAME,
        visible = Visible.TRUE,
        formula = lowerCaseFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to "John D, Smith",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `formula engine makes first character of every word uppercase and the rest lowercase when proper function is called`() {
    val lowerCaseFormula = "proper(\${name})"
    val row: Map<String, Any?> = mapOf(
      NAME to "JoHN D, sMiTh",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:$NAME",
        display = NAME,
        visible = Visible.TRUE,
        formula = lowerCaseFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to "John D, Smith",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `formula engine concats strings when the plus operator is used`() {
    val formula = "'Name: ' + \${name}"
    val row: Map<String, Any?> = mapOf(
      NAME to "John D, Smith",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:$NAME",
        display = NAME,
        visible = Visible.TRUE,
        formula = formula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to "Name: John D, Smith",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  @Test
  fun `Formula engine concatenates strings and variables inside make_url formula`() {
    val makeUrlFormula = "make_url('https://prisoner-' + \${env} + '.digital.prison.service.justice.gov.uk/prisoner/' + '\${prison_number}',\${name},TRUE)"
    val prisonNumber = "ABC123"
    val name = "LastName6, F"
    val row: Map<String, Any> = mapOf(
      NAME to name,
      PRISON_NUMBER to prisonNumber,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:destination",
        display = "Destination",
        visible = Visible.TRUE,
        formula = makeUrlFormula,
      ),
    )
    val expectedRow: Map<String, Any> = mapOf(
      NAME to name,
      PRISON_NUMBER to prisonNumber,
      DESTINATION to "<a href=\'https://prisoner-dev.digital.prison.service.justice.gov.uk/prisoner/${prisonNumber}\' target=\"_blank\">$name</a>",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(reportFields, "dev")
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }

  private fun testFormatNumber(formula: String, input: Number, expectedOutput: String) {
    val formatNumFormula = "format_number(\${money}, '$formula')"
    val name = "LastName6, F"
    val row: Map<String, Any?> = mapOf(
      NAME to name,
      MONEY to input,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val reportFields = listOf(
      ReportField(
        name = "\$ref:money",
        display = "Money",
        visible = Visible.TRUE,
        formula = formatNumFormula,
      ),
    )
    val expectedRow: Map<String, Any?> = mapOf(
      NAME to name,
      MONEY to expectedOutput,
      DESTINATION to "Manchester",
      DESTINATION_CODE to "MNCH",
    )
    val formulaEngine = FormulaEngine(reportFields)
    assertEquals(expectedRow, formulaEngine.applyFormulas(row))
  }
}
