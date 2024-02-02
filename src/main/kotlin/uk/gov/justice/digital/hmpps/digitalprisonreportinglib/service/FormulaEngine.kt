package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField

class FormulaEngine(private val reportFields: List<ReportField>) {

  fun applyFormulas(row: Map<String, Any>): Map<String, Any> =
    row.entries.associate { e ->
      e.key to (
        reportFields.firstOrNull { reportField -> reportField.name.removePrefix("\$ref:") == e.key }
          ?.formula?.ifEmpty { null }
          ?.let {
            interpolate(formula = it, row)
          } ?: e.value
        )
    }

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
