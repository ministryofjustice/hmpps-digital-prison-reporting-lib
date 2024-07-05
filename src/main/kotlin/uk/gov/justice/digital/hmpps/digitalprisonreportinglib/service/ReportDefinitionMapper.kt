package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.Specification
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.VariantDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.WordWrap
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.DynamicFilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FeatureType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Parameter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.StaticFilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Visible
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ConfiguredApiService.Companion.SCHEMA_REF_PREFIX
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.FormulaEngine.Companion.MAKE_URL_FORMULA_PREFIX
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.temporal.ChronoUnit

const val DEFAULT_MAX_STATIC_OPTIONS: Long = 30

@Component
class ReportDefinitionMapper(val configuredApiService: ConfiguredApiService) {

  val todayRegex: Regex = Regex("today\\(\\)")
  val dateRegex: Regex = Regex("today\\((-?\\d+), ?([a-z]+)\\)", RegexOption.IGNORE_CASE)
  fun map(definition: SingleReportProductDefinition, userToken: DprAuthAwareAuthenticationToken?, dataProductDefinitionsPath: String? = null): SingleVariantReportDefinition {
    return SingleVariantReportDefinition(
      id = definition.id,
      name = definition.name,
      description = definition.description,
      variant = map(
        report = definition.report,
        dataSet = definition.reportDataset,
        productDefinitionId = definition.id,
        userToken = userToken,
        dataProductDefinitionsPath = dataProductDefinitionsPath,
        filterDatasets = definition.filterDatasets,
      ),
    )
  }

  private fun map(
    report: Report,
    dataSet: Dataset,
    productDefinitionId: String,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String? = null,
    filterDatasets: List<Dataset>? = null,
  ): VariantDefinition {
    return VariantDefinition(
      id = report.id,
      name = report.name,
      description = report.description,
      specification = map(
        specification = report.specification,
        schemaFields = dataSet.schema.field,
        productDefinitionId = productDefinitionId,
        reportVariantId = report.id,
        userToken = userToken,
        dataProductDefinitionsPath = dataProductDefinitionsPath,
        filterDatasets = filterDatasets,
        parameters = dataSet.parameters,
      ),
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
    filterDatasets: List<Dataset>? = null,
    parameters: List<Parameter>? = null,
  ): Specification? {
    if (specification == null) {
      return null
    }
    return Specification(
      template = specification.template,
      sections = specification.section?.map { it.removePrefix(SCHEMA_REF_PREFIX) } ?: emptyList(),
      fields = mapToReportFieldDefinitions(
        specification,
        schemaFields,
        productDefinitionId,
        reportVariantId,
        userToken,
        dataProductDefinitionsPath,
        filterDatasets,
      ) + maybeConvertToReportFields(parameters),
    )
  }

  private fun maybeConvertToReportFields(parameters: List<Parameter>?) =
    parameters?.map { convert(it) } ?: emptyList()

  private fun mapToReportFieldDefinitions(
    specification: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Specification,
    schemaFields: List<SchemaField>,
    productDefinitionId: String,
    reportVariantId: String,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String?,
    filterDatasets: List<Dataset>?,
  ) = specification.field.map {
    map(
      field = it,
      schemaFields = schemaFields,
      productDefinitionId = productDefinitionId,
      reportVariantId = reportVariantId,
      userToken = userToken,
      dataProductDefinitionsPath = dataProductDefinitionsPath,
      filterDatasets = filterDatasets,
    )
  }

  private fun convert(parameter: Parameter): FieldDefinition {
    return FieldDefinition(
      name = parameter.name,
      display = parameter.display,
      sortable = false,
      defaultsort = false,
      type = convertParameterTypeToFieldType(parameter.reportFieldType),
      mandatory = parameter.mandatory,
      visible = true,
      filter = FilterDefinition(type = FilterType.valueOf(parameter.filterType.toString()), mandatory = parameter.mandatory),
    )
  }

  private fun map(
    field: ReportField,
    schemaFields: List<SchemaField>,
    productDefinitionId: String,
    reportVariantId: String,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String?,
    filterDatasets: List<Dataset>? = null,
  ): FieldDefinition {
    val schemaFieldRef = field.name.removePrefix(SCHEMA_REF_PREFIX)
    val schemaField = schemaFields.find { it.name == schemaFieldRef }
      ?: throw IllegalArgumentException("Could not find matching Schema Field '$schemaFieldRef'")

    return FieldDefinition(
      name = schemaField.name,
      display = populateDisplay(field.display, schemaField.display),
      wordWrap = field.wordWrap?.toString()?.let(WordWrap::valueOf),
      filter = field.filter?.let {
        map(
          filterDefinition = it,
          productDefinitionId = productDefinitionId,
          reportVariantId = reportVariantId,
          schemaFieldName = schemaField.name,
          userToken = userToken,
          dataProductDefinitionsPath = dataProductDefinitionsPath,
          filterDatasets = filterDatasets,
        )
      },
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
    if (reportField.formula?.startsWith(MAKE_URL_FORMULA_PREFIX) == true) {
      return FieldType.HTML
    }

    return convertParameterTypeToFieldType(schemaField.type)
  }

  private fun convertParameterTypeToFieldType(parameterType: ParameterType): FieldType {
    return when (parameterType) {
      ParameterType.Boolean -> FieldType.Boolean
      ParameterType.Date -> FieldType.Date
      ParameterType.DateTime -> FieldType.Date
      ParameterType.Timestamp -> FieldType.Date
      ParameterType.Time -> FieldType.Time
      ParameterType.Double -> FieldType.Double
      ParameterType.Float -> FieldType.Double
      ParameterType.Integer -> FieldType.Long
      ParameterType.Long -> FieldType.Long
      ParameterType.String -> FieldType.String
    }
  }

  private fun map(
    filterDefinition: uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition,
    productDefinitionId: String,
    reportVariantId: String,
    schemaFieldName: String,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String?,
    filterDatasets: List<Dataset>? = null,
  ): FilterDefinition {
    return FilterDefinition(
      type = FilterType.valueOf(filterDefinition.type.toString()),
      staticOptions = populateStaticOptions(
        filterDefinition = filterDefinition,
        productDefinitionId = productDefinitionId,
        reportVariantId = reportVariantId,
        schemaFieldName = schemaFieldName,
        maxStaticOptions = filterDefinition.dynamicOptions?.maximumOptions,
        userToken = userToken,
        dataProductDefinitionsPath = dataProductDefinitionsPath,
        filterDatasets = filterDatasets,
      ),
      dynamicOptions = filterDefinition.dynamicOptions,
      defaultValue = replaceTokens(filterDefinition.default),
      min = replaceTokens(filterDefinition.min),
      max = replaceTokens(filterDefinition.max),
      mandatory = filterDefinition.mandatory,
      pattern = filterDefinition.pattern,
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
    filterDatasets: List<Dataset>? = null,
  ): List<FilterOption>? {
    return filterDefinition.dynamicOptions?.takeIf { it.returnAsStaticOptions }?.let { dynamicFilterOption ->
      dynamicFilterOption.dataset?.let { dynamicFilterDatasetId ->
        return populateStaticOptionsForFilterWithDataset(
          dynamicFilterOption,
          filterDatasets,
          dynamicFilterDatasetId,
          productDefinitionId,
          reportVariantId,
          maxStaticOptions,
          userToken,
          dataProductDefinitionsPath,
        )
      }
        ?: populateStandardStaticOptionsForReportDefinition(
          productDefinitionId,
          reportVariantId,
          maxStaticOptions,
          schemaFieldName,
          userToken,
          dataProductDefinitionsPath,
        )
    } ?: filterDefinition.staticOptions?.map(this::map)
  }

  private fun populateStandardStaticOptionsForReportDefinition(
    productDefinitionId: String,
    reportVariantId: String,
    maxStaticOptions: Long?,
    schemaFieldName: String,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String?,
  ) = configuredApiService.validateAndFetchData(
    reportId = productDefinitionId,
    reportVariantId = reportVariantId,
    filters = emptyMap(),
    selectedPage = 1,
    pageSize = maxStaticOptions ?: DEFAULT_MAX_STATIC_OPTIONS,
    sortColumn = schemaFieldName,
    sortedAsc = true,
    userToken = userToken,
    reportFieldId = setOf(schemaFieldName),
    dataProductDefinitionsPath = dataProductDefinitionsPath,
  )
    .flatMap { it.entries }
    .map { FilterOption(it.value.toString(), it.value.toString()) }

  private fun populateStaticOptionsForFilterWithDataset(
    dynamicFilterOption: DynamicFilterOption,
    filterDatasets: List<Dataset>?,
    dynamicFilterDatasetId: String,
    productDefinitionId: String,
    reportVariantId: String,
    maxStaticOptions: Long?,
    userToken: DprAuthAwareAuthenticationToken?,
    dataProductDefinitionsPath: String?,
  ): List<FilterOption> {
    val schemaFieldRefForName = dynamicFilterOption.name?.removePrefix(SCHEMA_REF_PREFIX)
    val schemaFieldRefForDisplay = dynamicFilterOption.display?.removePrefix(SCHEMA_REF_PREFIX)
    val matchingFilterDataset = filterDatasets?.find { it.id == dynamicFilterDatasetId.removePrefix(SCHEMA_REF_PREFIX) }
    val matchingSchemaFieldsForFilterDataset = matchingFilterDataset?.schema?.field
    val nameSchemaField =
      matchingSchemaFieldsForFilterDataset?.find { it.name == schemaFieldRefForName } ?: throw IllegalArgumentException(
        "Could not find matching Schema Field '$schemaFieldRefForName'",
      )
    val displaySchemaField = matchingSchemaFieldsForFilterDataset.find { it.name == schemaFieldRefForDisplay }
      ?: throw IllegalArgumentException("Could not find matching Schema Field '$schemaFieldRefForDisplay'")
    return configuredApiService.validateAndFetchData(
      reportId = productDefinitionId,
      reportVariantId = reportVariantId,
      filters = emptyMap(),
      selectedPage = 1,
      pageSize = maxStaticOptions ?: DEFAULT_MAX_STATIC_OPTIONS,
      sortColumn = nameSchemaField.name,
      sortedAsc = true,
      userToken = userToken,
      reportFieldId = linkedSetOf(nameSchemaField.name, displaySchemaField.name),
      datasetForFilter = matchingFilterDataset,
      dataProductDefinitionsPath = dataProductDefinitionsPath,
    )
      .map { FilterOption(it[nameSchemaField.name].toString(), it[displaySchemaField.name].toString()) }
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
}
