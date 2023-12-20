package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Specification
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.VariantDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.WordWrap
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.StaticFilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.temporal.ChronoUnit

@Component
class ReportDefinitionMapper(val configuredApiService: ConfiguredApiService) {

  val todayRegex: Regex = Regex("today\\(\\)")
  val dateRegex: Regex = Regex("today\\((-?\\d+), ?([a-z]+)\\)", RegexOption.IGNORE_CASE)

  fun map(
    productDefinition: ProductDefinition,
    renderMethod: RenderMethod?,
    maxStaticOptions: Long,
    userToken: DprAuthAwareAuthenticationToken?,
  ): ReportDefinition = ReportDefinition(
    id = productDefinition.id,
    name = productDefinition.name,
    description = productDefinition.description,
    variants = productDefinition.report
      .filter { renderMethod == null || it.render.toString() == renderMethod.toString() }
      .map { map(productDefinition.id, it, productDefinition.dataset, maxStaticOptions, userToken) },
  )

  private fun map(productDefinitionId: String, report: Report, datasets: List<Dataset>, maxStaticOptions: Long, userToken: DprAuthAwareAuthenticationToken?): VariantDefinition {
    val dataSetRef = report.dataset.removePrefix("\$ref:")
    val dataSet = datasets.find { it.id == dataSetRef }
      ?: throw IllegalArgumentException("Could not find matching DataSet '$dataSetRef'")

    return map(report, dataSet, productDefinitionId, maxStaticOptions, userToken)
  }

  private fun map(
    report: Report,
    dataSet: Dataset,
    productDefinitionId: String,
    maxStaticOptions: Long,
    userToken: DprAuthAwareAuthenticationToken?,
  ): VariantDefinition {
    return VariantDefinition(
      id = report.id,
      name = report.name,
      description = report.description,
      specification = map(report.specification, dataSet.schema.field, productDefinitionId, report.id, maxStaticOptions, userToken),
      resourceName = "reports/$productDefinitionId/${report.id}",
    )
  }

  private fun map(
    specification: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Specification?,
    schemaFields: List<SchemaField>,
    productDefinitionId: String,
    reportVariantId: String,
    maxStaticOptions: Long,
    userToken: DprAuthAwareAuthenticationToken?,
  ): Specification? {
    if (specification == null) {
      return null
    }

    return Specification(
      template = specification.template,
      fields = specification.field.map { map(it, schemaFields, productDefinitionId, reportVariantId, maxStaticOptions, userToken) },
    )
  }

  private fun map(
    field: ReportField,
    schemaFields: List<SchemaField>,
    productDefinitionId: String,
    reportVariantId: String,
    maxStaticOptions: Long,
    userToken: DprAuthAwareAuthenticationToken?,
  ): FieldDefinition {
    val schemaFieldRef = field.name.removePrefix("\$ref:")
    val schemaField = schemaFields.find { it.name == schemaFieldRef }
      ?: throw IllegalArgumentException("Could not find matching Schema Field '$schemaFieldRef'")

    return FieldDefinition(
      name = schemaField.name,
      display = field.display,
      wordWrap = field.wordWrap?.toString()?.let(WordWrap::valueOf),
      filter = field.filter?.let { map(it, productDefinitionId, reportVariantId, schemaField.name, maxStaticOptions, userToken) },
      sortable = field.sortable,
      defaultsort = field.defaultSort,
      type = schemaField.type.toString().let(FieldType::valueOf),
    )
  }

  private fun map(
    filterDefinition: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition,
    productDefinitionId: String,
    reportVariantId: String,
    schemaFieldName: String,
    maxStaticOptions: Long,
    userToken: DprAuthAwareAuthenticationToken?,
  ): FilterDefinition {
    return FilterDefinition(
      type = FilterType.valueOf(filterDefinition.type.toString()),
      staticOptions = populateStaticOptions(filterDefinition, productDefinitionId, reportVariantId, schemaFieldName, maxStaticOptions, userToken),
      dynamicOptions = filterDefinition.dynamicOptions,
      defaultValue = replaceTokens(filterDefinition.default),
    )
  }

  private fun populateStaticOptions(
    filterDefinition: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition,
    productDefinitionId: String,
    reportVariantId: String,
    schemaFieldName: String,
    maxStaticOptions: Long,
    userToken: DprAuthAwareAuthenticationToken?,
  ): List<FilterOption>? {
    return filterDefinition.dynamicOptions?.takeIf { it.returnAsStaticOptions }?.let {
      configuredApiService.validateAndFetchData(
        productDefinitionId,
        reportVariantId, emptyMap(), 1, maxStaticOptions, schemaFieldName, true, userToken, schemaFieldName,
      ).flatMap { it.entries }.map { FilterOption(it.value as String, it.value as String) }
    } ?: filterDefinition.staticOptions?.map(this::map)
  }

  private fun replaceTokens(defaultValue: String?): String? {
    if (defaultValue == null) {
      return null
    }

    val today = LocalDate.now().format(ISO_LOCAL_DATE)
    var result = defaultValue.replace(todayRegex, today)

    dateRegex.findAll(result)
      .forEach {
        val offset = it.groupValues[1].toLong()
        val resultDate = LocalDate.now().plus(offset, ChronoUnit.valueOf(it.groupValues[2].uppercase()))

        result = result.replace(it.value, resultDate.format(ISO_LOCAL_DATE))
      }

    return result
  }

  private fun map(definition: StaticFilterOption): FilterOption = FilterOption(
    name = definition.name,
    display = definition.display,
  )

  fun map(definition: SingleReportProductDefinition, maxStaticOptions: Long, userToken: DprAuthAwareAuthenticationToken?): SingleVariantReportDefinition {
    return SingleVariantReportDefinition(
      id = definition.id,
      name = definition.name,
      description = definition.description,
      variant = map(definition.report, definition.dataset, definition.id, maxStaticOptions, userToken),
    )
  }
}
