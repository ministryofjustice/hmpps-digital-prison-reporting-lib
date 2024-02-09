package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField

class FormulaEngine(private val reportFields: List<ReportField>, private val env: String? = null) {

  companion object {
    const val MAKE_URL_FORMULA_PREFIX = "make_url("
  }

  fun applyFormulas(row: Map<String, Any?>): Map<String, Any?> =
    row.entries.associate { e ->
      e.key to constructValueWithFormulaInterpolationIfNeeded(e, row)
    }

  private fun constructValueWithFormulaInterpolationIfNeeded(
    e: Map.Entry<String, Any?>,
    row: Map<String, Any?>,
  ) = (
    findFormula(e.key)
      ?.let {
        interpolate(formula = it, row)
      } ?: e.value
    )

  private fun findFormula(columnName: String) =
    reportFields.firstOrNull { reportField -> reportField.name.removePrefix("\$ref:") == columnName }
      ?.formula?.ifEmpty { null }

  private fun interpolate(formula: String, row: Map<String, Any?>): String {
    return if (formula.startsWith(MAKE_URL_FORMULA_PREFIX)) interpolateUrlFormula(formula, row) else interpolateStandardFormula(formula, row)
  }

  private fun interpolateStandardFormula(formula: String, row: Map<String, Any?>): String {
    val sb = StringBuilder(formula)
    row.keys.forEach {
      sb.replace(
        0,
        sb.length,
        sb.toString()
          .replace("\${$it}", row.getOrElse(it) { "" }.toString()),
      )
    }
    return sb.toString()
  }

  private fun interpolateUrlFormula(formula: String, row: Map<String, Any?>): String {
    val interpolatedEnv =
      env?.let { formula.replace("\${env}", env) } ?: formula.replace("-\${env}", "")
    val (hrefPlaceholder, linkTextPlaceholder, newTab) = interpolatedEnv.substring(MAKE_URL_FORMULA_PREFIX.length, interpolatedEnv.indexOf(")"))
      .split(",")
    val href = interpolateStandardFormula(hrefPlaceholder, row)
    val linkText = interpolateStandardFormula(linkTextPlaceholder, row)
    return """<a href=$href ${if (newTab.uppercase() == "TRUE") "target=\"_blank\"" else ""}>$linkText</a>"""
  }
}
