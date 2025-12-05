package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class FormulaEngine(
  private val reportFields: List<ReportField>,
  private val env: String? = null,
  private val identifiedHelper: IdentifiedHelper = IdentifiedHelper(),
) {

  companion object {
    const val MAKE_URL_FORMULA_PREFIX = "make_url("
    const val FORMAT_DATE_FORMULA_PREFIX = "format_date("
    const val FORMAT_NUMBER_FORMULA_PREFIX = "format_number("
    const val DEFAULT_VALUE_FORMULA_PREFIX = "default_value("
    const val LOWER_FORMULA_PREFIX = "lower("
    const val UPPER_FORMULA_PREFIX = "upper("
    const val WORDCAP_FORMULA_PREFIX = "wordcap("
    const val PROPER_FORMULA_PREFIX = "proper("
  }

  fun applyFormulas(row: Map<String, Any?>): Map<String, Any?> = row.entries.associate { e ->
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

  private fun findFormula(columnName: String) = identifiedHelper.findOrNull(reportFields, columnName)?.formula?.ifEmpty { null }

  private fun interpolate(formula: String, row: Map<String, Any?>): String = when {
    formula.startsWith(MAKE_URL_FORMULA_PREFIX) -> interpolateUrlFormula(formula, row)
    formula.startsWith(FORMAT_DATE_FORMULA_PREFIX) -> interpolateFormatDateFormula(formula, row)
    formula.startsWith(FORMAT_NUMBER_FORMULA_PREFIX) -> interpolateFormatNumberFormula(formula, row)
    formula.startsWith(DEFAULT_VALUE_FORMULA_PREFIX) -> interpolateDefaultValueFormula(formula, row)
    formula.startsWith(LOWER_FORMULA_PREFIX) -> interpolateLowerFormula(formula, row)
    formula.startsWith(UPPER_FORMULA_PREFIX) -> interpolateUpperFormula(formula, row)
    formula.startsWith(WORDCAP_FORMULA_PREFIX) -> interpolateWordcapFormula(formula, row)
    formula.startsWith(PROPER_FORMULA_PREFIX) -> interpolateProperFormula(formula, row)
    else -> interpolateStandardFormula(formula, row)
  }

  private fun interpolateFormatDateFormula(formula: String, row: Map<String, Any?>): String {
    val (dateColumnNamePlaceholder, datePatternPlaceholder) = formula.substring(FORMAT_DATE_FORMULA_PREFIX.length, formula.indexOf(")"))
      .split(",")
    val dateColumnName = dateColumnNamePlaceholder.removeSurrounding(prefix = "\${", suffix = "}")
    val date = row[dateColumnName] ?: return ""
    return when (date) {
      is LocalDate -> date.format(DateTimeFormatter.ofPattern(removeQuotes(datePatternPlaceholder.trim())))
      is LocalDateTime -> date.format(DateTimeFormatter.ofPattern(removeQuotes(datePatternPlaceholder.trim())))
      is Date -> SimpleDateFormat(removeQuotes(datePatternPlaceholder.trim())).format(date)
      else -> throw IllegalArgumentException("Could not parse date: $date, of type ${date::class}")
    }
  }

  private fun interpolateFormatNumberFormula(formula: String, row: Map<String, Any?>): String {
    val (numColumnNamePlaceholder, numPatternPlaceholder) = formula.substring(FORMAT_NUMBER_FORMULA_PREFIX.length, formula.indexOf(")"))
      .split(",", limit = 2)
    val numColumnName = numColumnNamePlaceholder.removeSurrounding(prefix = "\${", suffix = "}")
    val number = row[numColumnName] ?: return ""
    return when (number) {
      is Number -> DecimalFormat(removeQuotes(numPatternPlaceholder.trim())).format(number)
      else -> throw IllegalArgumentException("Could not parse number: $number, of type ${number::class}")
    }
  }

  private fun removeQuotes(dateFormat: String) = dateFormat.removeSurrounding("'", "'").removeSurrounding("\"", "\"")

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

  private fun interpolateDefaultValueFormula(formula: String, row: Map<String, Any?>): String {
    val (parameterToCheck, defaultValue) = formula.substring(DEFAULT_VALUE_FORMULA_PREFIX.length, formula.indexOf(")"))
      .split(",")
    val interpolatedValue = interpolateStandardFormula(parameterToCheck, row)
    return interpolatedValue.ifBlank { defaultValue.removeSurrounding("\'", "\'") }
  }

  private fun interpolateLowerFormula(formula: String, row: Map<String, Any?>): String {
    val originalCase = formula.substring(LOWER_FORMULA_PREFIX.length, formula.indexOf(")"))
    return interpolateStandardFormula(originalCase, row).lowercase()
  }

  private fun interpolateUpperFormula(formula: String, row: Map<String, Any?>): String {
    val originalCase = formula.substring(UPPER_FORMULA_PREFIX.length, formula.indexOf(")"))
    return interpolateStandardFormula(originalCase, row).uppercase()
  }

  private fun interpolateWordcapFormula(formula: String, row: Map<String, Any?>): String {
    val originalCase = formula.substring(WORDCAP_FORMULA_PREFIX.length, formula.indexOf(")"))
    return applyProperAndWordCapCase(originalCase, row)
  }

  private fun interpolateProperFormula(formula: String, row: Map<String, Any?>): String {
    val originalCase = formula.substring(PROPER_FORMULA_PREFIX.length, formula.indexOf(")"))
    return applyProperAndWordCapCase(originalCase, row)
  }

  private fun applyProperAndWordCapCase(
    originalCase: String,
    row: Map<String, Any?>,
  ): String = interpolateStandardFormula(originalCase, row).split(" ").joinToString(" ") { it.lowercase().replaceFirstChar { s -> s.uppercase() } }
}
