package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovementOriginCaseloadDirectionIn
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Visible

class FormulaEngineTest {

  companion object {
    private const val NAME = "name"
    private const val DATE = "date"
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
}
