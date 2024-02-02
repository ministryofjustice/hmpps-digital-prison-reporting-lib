package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepositoryTest.AllMovements.externalMovementOriginCaseloadDirectionIn
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField

class FormulaEngineTest {

  companion object {
    private const val NAME = "name"
    private const val DATE = "date"
    private const val DESTINATION = "destination"
    private const val DESTINATION_CODE = "destination_code"
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
        visible = true,
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
        visible = true,
        formula = "\${destination}:\${destination_code}:\${name}",
      ),
      ReportField(
        name = "\$ref:destination",
        display = "Destination",
        visible = true,
      ),
      ReportField(
        name = "\$ref:name",
        display = "Name",
        visible = true,
      ),
      ReportField(
        name = "\$ref:date",
        display = "Date",
        visible = true,
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
        visible = true,
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
        visible = true,
        formula = "",
      ),
      ReportField(
        name = "\$ref:destination",
        display = "Destination",
        visible = true,
      ),
      ReportField(
        name = "\$ref:name",
        display = "Name",
        visible = true,
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
        visible = true,
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
}
