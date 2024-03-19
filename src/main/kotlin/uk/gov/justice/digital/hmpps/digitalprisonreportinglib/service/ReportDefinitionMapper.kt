package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import com.google.gson.annotations.SerializedName
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
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.*
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.FormulaEngine.Companion.MAKE_URL_FORMULA_PREFIX
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.temporal.ChronoUnit

const val DEFAULT_MAX_STATIC_OPTIONS: Long = 30

@Component
class ReportDefinitionMapper(val configuredApiService: ConfiguredApiService) {

  val todayRegex: Regex = Regex("today\\(\\)")
  val dateRegex: Regex = Regex("today\\((-?\\d+), ?([a-z]+)\\)", RegexOption.IGNORE_CASE)

  fun map(
    productDefinition: ProductDefinition,
    renderMethod: RenderMethod?,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String? = null,
  ): ReportDefinition = ReportDefinition(
    id = productDefinition.id,
    name = productDefinition.name,
    description = productDefinition.description,
    variants = productDefinition.report
      .filter { renderMethod == null || it.render.toString() == renderMethod.toString() }
      .map { map(productDefinition.id, it, productDefinition.dataset, userToken, dataProductDefinitionsPath) },
  )

  private fun map(productDefinitionId: String, report: Report, datasets: List<Dataset>, userToken: DprAuthAwareAuthenticationToken?, dataProductDefinitionsPath: String? = null): VariantDefinition {
    val dataSetRef = report.dataset.removePrefix("\$ref:")
    val dataSet = datasets.find { it.id == dataSetRef }
      ?: throw IllegalArgumentException("Could not find matching DataSet '$dataSetRef'")

    return map(report, dataSet, productDefinitionId, userToken, dataProductDefinitionsPath)
  }

  private fun map(
    report: Report,
    dataSet: Dataset,
    productDefinitionId: String,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String? = null,
  ): VariantDefinition {
    return VariantDefinition(
      id = report.id,
      name = report.name,
      description = report.description,
      specification = map(report.specification, dataSet.schema.field, productDefinitionId, report.id, userToken, dataProductDefinitionsPath),
      classification = report.classification,
      printable = report.feature?.any { it.type == FeatureType.PRINT } ?: false,
      resourceName = "reports/$productDefinitionId/${report.id}",
    )
  }

  private fun map(
    specification: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Specification?,
    schemaFields: List<SchemaField>,
    productDefinitionId: String,
    reportVariantId: String,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String?,
  ): Specification? {
    if (specification == null) {
      return null
    }

    return Specification(
      template = specification.template,
      fields = specification.field.map { map(it, schemaFields, productDefinitionId, reportVariantId, userToken, dataProductDefinitionsPath) },
    )
  }

  private fun map(
    field: ReportField,
    schemaFields: List<SchemaField>,
    productDefinitionId: String,
    reportVariantId: String,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String?,
  ): FieldDefinition {
    val schemaFieldRef = field.name.removePrefix("\$ref:")
    val schemaField = schemaFields.find { it.name == schemaFieldRef }
      ?: throw IllegalArgumentException("Could not find matching Schema Field '$schemaFieldRef'")

    return FieldDefinition(
      name = schemaField.name,
      display = populateDisplay(field.display, schemaField.display),
      wordWrap = field.wordWrap?.toString()?.let(WordWrap::valueOf),
      filter = field.filter?.let { map(it, productDefinitionId, reportVariantId, schemaField.name, userToken, dataProductDefinitionsPath) },
      sortable = field.sortable,
      defaultsort = field.defaultSort,
      type = populateType(schemaField, field),
      mandatory = populateMandatory(field.visible),
      visible = populateVisible(field.visible),
      calculated = field.formula?.isNotBlank() ?: false,
    )
  }

  private fun populateDisplay(reportFieldDisplay: String?, schemaFieldDisplay: String): String {
    return reportFieldDisplay?.ifBlank { schemaFieldDisplay } ?: schemaFieldDisplay
  }

  private fun populateVisible(visible: Visible?): Boolean {
    return visible?.let {
      when (visible) {
        Visible.TRUE -> true
        Visible.FALSE -> false
        Visible.MANDATORY -> true
      }
    } ?: true
  }

  private fun populateMandatory(visible: Visible?): Boolean {
    return visible?.let {
      when (visible) {
        Visible.TRUE -> false
        Visible.FALSE -> false
        Visible.MANDATORY -> true
      }
    } ?: false
  }

  private fun populateType(schemaField: SchemaField, reportField: ReportField): FieldType {
    if (reportField.formula?.startsWith(MAKE_URL_FORMULA_PREFIX) ?: false) {
      return FieldType.HTML
    }

    when(schemaField.type) {
      ParameterType.Boolean -> return FieldType.Boolean
      ParameterType.Date -> return FieldType.Date
      ParameterType.DateTime -> return FieldType.Date
      ParameterType.Timestamp -> return FieldType.Date
      ParameterType.Time -> return FieldType.Time
      ParameterType.Double -> return FieldType.Double
      ParameterType.Float -> return FieldType.Double
      ParameterType.Integer -> return FieldType.Long
      ParameterType.Long -> return FieldType.Long
      ParameterType.String -> return FieldType.String
    }
  }

  private fun map(
    filterDefinition: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition,
    productDefinitionId: String,
    reportVariantId: String,
    schemaFieldName: String,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String?,
  ): FilterDefinition {
    return FilterDefinition(
      type = FilterType.valueOf(filterDefinition.type.toString()),
      staticOptions = populateStaticOptions(filterDefinition, productDefinitionId, reportVariantId, schemaFieldName, filterDefinition.dynamicOptions?.maximumOptions, userToken, dataProductDefinitionsPath),
      dynamicOptions = filterDefinition.dynamicOptions,
      defaultValue = replaceTokens(filterDefinition.default),
      min = replaceTokens(filterDefinition.min),
      max = replaceTokens(filterDefinition.max),
    )
  }

  private fun populateStaticOptions(
    filterDefinition: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition,
    productDefinitionId: String,
    reportVariantId: String,
    schemaFieldName: String,
    maxStaticOptions: Long?,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String?,
  ): List<FilterOption>? {
    return filterDefinition.dynamicOptions?.takeIf { it.returnAsStaticOptions }?.let {
      configuredApiService.validateAndFetchData(
        reportId = productDefinitionId,
        reportVariantId = reportVariantId,
        filters = emptyMap(),
        selectedPage = 1,
        pageSize = maxStaticOptions ?: DEFAULT_MAX_STATIC_OPTIONS,
        sortColumn = schemaFieldName,
        sortedAsc = true,
        userToken = userToken,
        reportFieldId = schemaFieldName,
        dataProductDefinitionsPath = dataProductDefinitionsPath,
      ).flatMap { it.entries }.map { FilterOption(it.value.toString(), it.value.toString()) }
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

  fun map(definition: SingleReportProductDefinition, userToken: DprAuthAwareAuthenticationToken?, dataProductDefinitionsPath: String? = null): SingleVariantReportDefinition {
    return SingleVariantReportDefinition(
      id = definition.id,
      name = definition.name,
      description = definition.description,
      variant = map(report = definition.report, dataSet = definition.dataset, productDefinitionId = definition.id, userToken = userToken, dataProductDefinitionsPath = dataProductDefinitionsPath),
    )
  }
}
