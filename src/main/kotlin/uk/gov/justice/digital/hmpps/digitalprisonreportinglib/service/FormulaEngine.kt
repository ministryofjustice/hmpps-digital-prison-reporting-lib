package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField

class FormulaEngine(private val reportFields: List<ReportField>) {

  fun applyFormulas(row: Map<String, Any>): Map<String, Any> =
    row.entries.associate { e ->
      e.key to constructValueWithFormulaInterpolationIfNeeded(e, row)
    }

  private fun constructValueWithFormulaInterpolationIfNeeded(
    e: Map.Entry<String, Any>,
    row: Map<String, Any>,
  ) = (
    findFormulas(e.key)
      ?.let {
        interpolate(formula = it, row)
      } ?: e.value
    )

  private fun findFormulas(columnName: String) =
    reportFields.firstOrNull { reportField -> reportField.name.removePrefix("\$ref:") == columnName }
      ?.formula?.ifEmpty { null }

  private fun interpolate(formula: String, row: Map<String, Any>): String {
    val sb = StringBuilder(formula)
    row.keys.forEach {
      sb.replace(
        0,
        sb.length,
        sb.toString()
          .replace("\${$it}", row.getOrDefault(it, "").toString()),
      )
    }
    return sb.toString()
  }
}
