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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.DataSet
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.temporal.ChronoUnit

@Component
class ReportDefinitionMapper {

  val todayRegex: Regex = Regex("today\\(\\)")
  val dateRegex: Regex = Regex("today\\((-?\\d+), ?([a-z]+)\\)", RegexOption.IGNORE_CASE)

  fun map(productDefinition: ProductDefinition, renderMethod: RenderMethod?): ReportDefinition = ReportDefinition(
    id = productDefinition.id,
    name = productDefinition.name,
    description = productDefinition.description,
    variants = productDefinition.report
      .filter { renderMethod == null || it.render.toString() == renderMethod.toString() }
      .map { map(productDefinition.id, it, productDefinition.dataSet) },
  )

  private fun map(productDefinitionId: String, report: Report, dataSets: List<DataSet>): VariantDefinition {
    val dataSetRef = report.dataset.removePrefix("\$ref:")
    val dataSet = dataSets.find { it.id == dataSetRef }
      ?: throw IllegalArgumentException("Could not find matching DataSet '$dataSetRef'")

    return map(report, dataSet, productDefinitionId)
  }

  private fun map(
    report: Report,
    dataSet: DataSet,
    productDefinitionId: String,
  ): VariantDefinition {
    return VariantDefinition(
      id = report.id,
      name = report.name,
      description = report.description,
      specification = map(report.specification, dataSet.schema.field),
      resourceName = "reports/$productDefinitionId/${report.id}",
    )
  }

  private fun map(
    specification: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Specification?,
    schemaFields: List<SchemaField>,

  ): Specification? {
    if (specification == null) {
      return null
    }

    return Specification(
      template = specification.template,
      fields = specification.field.map { map(it, schemaFields) },
    )
  }

  private fun map(field: ReportField, schemaFields: List<SchemaField>): FieldDefinition {
    val schemaFieldRef = field.schemaField.removePrefix("\$ref:")
    val schemaField = schemaFields.find { it.name == schemaFieldRef }
      ?: throw IllegalArgumentException("Could not find matching Schema Field '$schemaFieldRef'")

    return FieldDefinition(
      name = schemaField.name,
      displayName = field.displayName,
      wordWrap = field.wordWrap?.toString()?.let(WordWrap::valueOf),
      filter = field.filter?.let(this::map),
      sortable = field.sortable,
      defaultSortColumn = field.defaultSortColumn,
      type = schemaField.type.toString().let(FieldType::valueOf),
    )
  }

  private fun map(definition: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition): FilterDefinition = FilterDefinition(
    type = FilterType.valueOf(definition.type.toString()),
    staticOptions = definition.staticOptions?.map(this::map),
    defaultValue = replaceTokens(definition.defaultValue),
  )

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

  private fun map(definition: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterOption): FilterOption = FilterOption(
    name = definition.name,
    displayName = definition.displayName,
  )

  fun map(definition: SingleReportProductDefinition): SingleVariantReportDefinition {
    return SingleVariantReportDefinition(
      id = definition.id,
      name = definition.name,
      description = definition.description,
      variant = map(definition.report, definition.dataSet, definition.id),
    )
  }
}
