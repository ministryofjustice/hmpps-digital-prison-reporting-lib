package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod.HTML
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.DynamicFilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Feature
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FeatureType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MetaData
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Schema
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Specification
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.StaticFilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.WordWrap
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Effect
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType.ROW_LEVEL
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Rule
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Collections.singletonMap

class ReportDefinitionMapperTest {

  private val fullDataset = Dataset(
    id = "10",
    name = "11",
    query = "12",
    schema = Schema(
      field = listOf(
        SchemaField(
          name = "13",
          type = ParameterType.Long,
        ),
      ),
    ),
  )

  private val fullDatasource = Datasource(
    id = "18",
    name = "19",
  )

  private val feature = Feature(
    type = FeatureType.PRINT.type,
  )

  private val fullReport = Report(
    id = "21",
    name = "22",
    description = "23",
    created = LocalDateTime.MAX,
    version = "24",
    dataset = "\$ref:10",
    render = RenderMethod.PDF,
    schedule = "26",
    specification = Specification(
      template = "27",
      field = listOf(
        ReportField(
          name = "\$ref:13",
          display = "14",
          wordWrap = WordWrap.None,
          filter = FilterDefinition(
            type = FilterType.Radio,
            staticOptions = listOf(
              StaticFilterOption(
                name = "16",
                display = "17",
              ),
            ),
          ),
          sortable = true,
          defaultSort = true,
          formula = null,
          visible = true,
        ),
      ),
    ),
    destination = listOf(singletonMap("28", "29")),
    classification = "someClassification",
    feature = listOf(feature),
  )

  private val fullProductDefinition: ProductDefinition = ProductDefinition(
    id = "1",
    name = "2",
    description = "3",
    metadata = MetaData(
      author = "4",
      version = "5",
      owner = "6",
      purpose = "7",
      profile = "8",
      dqri = "9",
    ),
    dataset = listOf(fullDataset),
    datasource = listOf(fullDatasource),
    report = listOf(fullReport),
  )

  private val policy: Policy = Policy(
    "caseload",
    ROW_LEVEL,
    listOf("(origin_code=\${caseload} AND direction='OUT') OR (destination_code=\${caseload} AND direction='IN')"),
    listOf(Rule(Effect.PERMIT, emptyList())),
  )

  private val fullSingleReportProductDefinition: SingleReportProductDefinition = SingleReportProductDefinition(
    id = "1",
    name = "2",
    description = "3",
    metadata = MetaData(
      author = "4",
      version = "5",
      owner = "6",
      purpose = "7",
      profile = "8",
      dqri = "9",
    ),
    datasource = fullDatasource,
    dataset = fullDataset,
    report = fullReport,
    policy = listOf(policy),
  )

  private val configuredApiService: ConfiguredApiService = mock()

  private val authToken = mock<DprAuthAwareAuthenticationToken>()

  @Test
  fun `Getting report list for user maps full data correctly`() {
    val mapper = ReportDefinitionMapper(configuredApiService)

    val result = mapper.map(fullProductDefinition, null, authToken)

    assertThat(result).isNotNull
    assertThat(result.id).isEqualTo(fullProductDefinition.id)
    assertThat(result.name).isEqualTo(fullProductDefinition.name)
    assertThat(result.description).isEqualTo(fullProductDefinition.description)
    assertThat(result.variants).isNotEmpty
    assertThat(result.variants).hasSize(1)

    val variant = result.variants.first()

    assertThat(variant.id).isEqualTo(fullProductDefinition.report.first().id)
    assertThat(variant.name).isEqualTo(fullProductDefinition.report.first().name)
    assertThat(variant.resourceName).isEqualTo("reports/${fullProductDefinition.id}/${fullProductDefinition.report.first().id}")
    assertThat(variant.description).isEqualTo(fullProductDefinition.report.first().description)
    assertThat(variant.specification).isNotNull
    assertThat(variant.classification).isEqualTo(fullProductDefinition.report.first().classification)
    assertThat(variant.printable).isEqualTo(fullProductDefinition.report.first().feature.first().type == FeatureType.PRINT.type)
    assertThat(variant.specification?.template).isEqualTo(fullProductDefinition.report.first().specification?.template)
    assertThat(variant.specification?.fields).isNotEmpty
    assertThat(variant.specification?.fields).hasSize(1)

    val field = variant.specification!!.fields.first()
    val sourceSchemaField = fullProductDefinition.dataset.first().schema.field.first()
    val sourceReportField = fullProductDefinition.report.first().specification!!.field.first()

    assertThat(field.name).isEqualTo(sourceSchemaField.name)
    assertThat(field.display).isEqualTo(sourceReportField.display)
    assertThat(field.wordWrap.toString()).isEqualTo(sourceReportField.wordWrap.toString())
    assertThat(field.sortable).isEqualTo(sourceReportField.sortable)
    assertThat(field.defaultsort).isEqualTo(sourceReportField.defaultSort)
    assertThat(field.filter).isNotNull
    assertThat(field.filter?.type.toString()).isEqualTo(sourceReportField.filter?.type.toString())
    assertThat(field.filter?.staticOptions).isNotEmpty
    assertThat(field.filter?.staticOptions).hasSize(1)
    assertThat(field.type.toString()).isEqualTo(sourceSchemaField.type.toString())

    val filterOption = field.filter?.staticOptions?.first()
    val sourceFilterOption = sourceReportField.filter?.staticOptions?.first()

    assertThat(filterOption?.name).isEqualTo(sourceFilterOption?.name)
    assertThat(filterOption?.display).isEqualTo(sourceFilterOption?.display)
    verifyNoInteractions(configuredApiService)
  }

  @Test
  fun `Getting report list for user maps minimal data successfully`() {
    val productDefinition = ProductDefinition(
      id = "1",
      name = "2",
      metadata = MetaData(
        author = "3",
        owner = "4",
        version = "5",
      ),
    )
    val mapper = ReportDefinitionMapper(configuredApiService)

    val result = mapper.map(productDefinition, null, authToken)

    assertThat(result).isNotNull
    assertThat(result.variants).hasSize(0)
    verifyNoInteractions(configuredApiService)
  }

  @Test
  fun `Getting report list for statically returned dynamic filter values on a number succeeds`() {
    whenever(
      configuredApiService.validateAndFetchData(any(), any(), any(), anyLong(), anyLong(), any(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull()),
    ).thenReturn(listOf(mapOf("1" to BigDecimal(1)), mapOf("2" to BigDecimal(2))))

    val productDefinition = ProductDefinition(
      id = "1",
      name = "2",
      metadata = MetaData(
        author = "3",
        owner = "4",
        version = "5",
      ),
      dataset = listOf(fullDataset),
      report = listOf(
        Report(
          id = "21",
          name = "22",
          description = "23",
          created = LocalDateTime.MAX,
          version = "24",
          dataset = "\$ref:10",
          render = RenderMethod.PDF,
          schedule = "26",
          specification = Specification(
            template = "27",
            field = listOf(
              ReportField(
                name = "\$ref:13",
                display = "14",
                wordWrap = WordWrap.None,
                filter = FilterDefinition(
                  type = FilterType.Radio,
                  dynamicOptions = DynamicFilterOption(
                    returnAsStaticOptions = true,
                  ),
                ),
                sortable = true,
                defaultSort = true,
                formula = null,
                visible = true,
              ),
            ),
          ),
          destination = listOf(singletonMap("28", "29")),
          classification = "someClassification",
        ),
      ),
    )
    val mapper = ReportDefinitionMapper(configuredApiService)

    val result = mapper.map(productDefinition, null, authToken)

    assertThat(result.variants[0].specification!!.fields[0].filter!!.staticOptions).hasSize(2)
  }

  @Test
  fun `Getting report list for user fails when mapping report with no matching dataset`() {
    val productDefinition = ProductDefinition(
      id = "1",
      name = "2",
      metadata = MetaData(
        author = "3",
        owner = "4",
        version = "5",
      ),
      report = listOf(
        Report(
          id = "6",
          name = "7",
          created = LocalDateTime.MAX,
          version = "8",
          dataset = "\$ref:9",
          render = RenderMethod.SVG,
          classification = "someClassification",
        ),
      ),
    )
    val mapper = ReportDefinitionMapper(configuredApiService)

    val exception = assertThrows(IllegalArgumentException::class.java) {
      mapper.map(productDefinition, null, authToken)
    }
    verifyNoInteractions(configuredApiService)
    assertThat(exception).message().isEqualTo("Could not find matching DataSet '9'")
  }

  @Test
  fun `Getting HTML report list returns relevant reports`() {
    val productDefinition = ProductDefinition(
      id = "1",
      name = "2",
      metadata = MetaData(
        author = "3",
        owner = "4",
        version = "5",
      ),
      dataset = listOf(
        Dataset(
          id = "10",
          name = "11",
          query = "12",
          schema = Schema(
            field = emptyList(),
          ),
        ),
      ),
      report = listOf(
        Report(
          id = "13",
          name = "14",
          created = LocalDateTime.MAX,
          version = "15",
          dataset = "\$ref:10",
          render = RenderMethod.SVG,
          classification = "someClassification",
        ),
        Report(
          id = "16",
          name = "17",
          created = LocalDateTime.MAX,
          version = "18",
          dataset = "\$ref:10",
          render = RenderMethod.HTML,
          classification = "someClassification",
        ),
      ),
    )
    val mapper = ReportDefinitionMapper(configuredApiService)

    val result = mapper.map(productDefinition, HTML, authToken)

    assertThat(result).isNotNull
    assertThat(result.variants).hasSize(1)
    assertThat(result.variants.first().id).isEqualTo("16")
    verifyNoInteractions(configuredApiService)
  }

  @ParameterizedTest
  @CsvSource(
    "-2,DAYS",
    "-1,DAYS",
    "0,DAYS",
    "1,DAYS",
    "2,DAYS",
    "2,WEEKS",
    "2,MONTHS",
    "2,YEARS",
  )
  fun `Default value token is mapped correctly`(offset: Long, magnitude: ChronoUnit) {
    val defaultValue = createProductDefinitionWithDefaultFilter("today($offset, $magnitude)")
    val expectedDate = getExpectedDate(offset, magnitude)

    val result = ReportDefinitionMapper(configuredApiService).map(defaultValue, HTML, authToken)

    assertThat(result.variants[0].specification!!.fields[0].filter!!.defaultValue).isEqualTo(expectedDate)

    verifyNoInteractions(configuredApiService)
  }

  @Test
  fun `Default value token for today is mapped correctly`() {
    val defaultValue = createProductDefinitionWithDefaultFilter("today()")
    val expectedDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val result = ReportDefinitionMapper(configuredApiService).map(defaultValue, HTML, authToken)

    assertThat(result.variants[0].specification!!.fields[0].filter!!.defaultValue).isEqualTo(expectedDate)

    verifyNoInteractions(configuredApiService)
  }

  @Test
  fun `Multiple default value tokens are mapped correctly`() {
    val defaultValue = createProductDefinitionWithDefaultFilter("today(-7,DAYS), today(), today(7,DAYS)")
    val expectedDate1 = getExpectedDate(-7, ChronoUnit.DAYS)
    val expectedDate2 = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val expectedDate3 = getExpectedDate(7, ChronoUnit.DAYS)
    val expectedResult = "$expectedDate1, $expectedDate2, $expectedDate3"

    val result = ReportDefinitionMapper(configuredApiService).map(defaultValue, HTML, authToken)

    assertThat(result.variants[0].specification!!.fields[0].filter!!.defaultValue).isEqualTo(expectedResult)

    verifyNoInteractions(configuredApiService)
  }

  @Test
  fun `Getting single report for user maps full data correctly`() {
    val mapper = ReportDefinitionMapper(configuredApiService)

    val result = mapper.map(fullSingleReportProductDefinition, authToken)

    assertThat(result).isNotNull
    assertThat(result.id).isEqualTo(fullSingleReportProductDefinition.id)
    assertThat(result.name).isEqualTo(fullSingleReportProductDefinition.name)
    assertThat(result.description).isEqualTo(fullSingleReportProductDefinition.description)

    val variant = result.variant

    assertThat(variant.id).isEqualTo(fullSingleReportProductDefinition.report.id)
    assertThat(variant.name).isEqualTo(fullSingleReportProductDefinition.report.name)
    assertThat(variant.resourceName).isEqualTo("reports/${fullSingleReportProductDefinition.id}/${fullSingleReportProductDefinition.report.id}")
    assertThat(variant.description).isEqualTo(fullSingleReportProductDefinition.report.description)
    assertThat(variant.specification).isNotNull
    assertThat(variant.specification?.template).isEqualTo(fullSingleReportProductDefinition.report.specification?.template)
    assertThat(variant.specification?.fields).isNotEmpty
    assertThat(variant.specification?.fields).hasSize(1)

    val field = variant.specification!!.fields.first()
    val sourceSchemaField = fullSingleReportProductDefinition.dataset.schema.field.first()
    val sourceReportField = fullSingleReportProductDefinition.report.specification!!.field.first()

    assertThat(field.name).isEqualTo(sourceSchemaField.name)
    assertThat(field.display).isEqualTo(sourceReportField.display)
    assertThat(field.wordWrap.toString()).isEqualTo(sourceReportField.wordWrap.toString())
    assertThat(field.sortable).isEqualTo(sourceReportField.sortable)
    assertThat(field.defaultsort).isEqualTo(sourceReportField.defaultSort)
    assertThat(field.filter).isNotNull
    assertThat(field.filter?.type.toString()).isEqualTo(sourceReportField.filter?.type.toString())
    assertThat(field.filter?.staticOptions).isNotEmpty
    assertThat(field.filter?.staticOptions).hasSize(1)
    assertThat(field.type.toString()).isEqualTo(sourceSchemaField.type.toString())

    val filterOption = field.filter?.staticOptions?.first()
    val sourceFilterOption = sourceReportField.filter?.staticOptions?.first()

    assertThat(filterOption?.name).isEqualTo(sourceFilterOption?.name)
    assertThat(filterOption?.display).isEqualTo(sourceFilterOption?.display)

    verifyNoInteractions(configuredApiService)
  }

  @Test
  fun `getting single report with dynamic options maps full data correctly and generates the static options in the result when returnAsStaticOptions is true`() {
    val reportWithDynamicFilter = Report(
      id = "21",
      name = "22",
      description = "23",
      created = LocalDateTime.MAX,
      version = "24",
      dataset = "\$ref:10",
      render = RenderMethod.PDF,
      schedule = "26",
      specification = Specification(
        template = "27",
        field = listOf(
          ReportField(
            name = "\$ref:13",
            display = "14",
            wordWrap = WordWrap.None,
            filter = FilterDefinition(
              type = FilterType.AutoComplete,
              dynamicOptions = DynamicFilterOption(minimumLength = 2, returnAsStaticOptions = true),
            ),
            sortable = true,
            defaultSort = true,
            formula = null,
            visible = true,
          ),
        ),
      ),
      destination = listOf(singletonMap("28", "29")),
      classification = "someClassification",
    )

    val fullSingleProductDefinition = fullSingleReportProductDefinition.copy(report = reportWithDynamicFilter)

    val mapper = ReportDefinitionMapper(configuredApiService)

    whenever(
      configuredApiService.validateAndFetchData(
        fullSingleProductDefinition.id,
        reportWithDynamicFilter.id, emptyMap(), 1, DEFAULT_MAX_STATIC_OPTIONS, "13", true, authToken, "13",
      ),
    ).thenReturn(listOf(mapOf("13" to "static1"), mapOf("13" to "static2")))

    val result = mapper.map(fullSingleProductDefinition, authToken)

    assertThat(result).isNotNull
    assertThat(result.id).isEqualTo(fullSingleProductDefinition.id)
    assertThat(result.name).isEqualTo(fullSingleProductDefinition.name)
    assertThat(result.description).isEqualTo(fullSingleProductDefinition.description)

    val variant = result.variant

    assertThat(variant.id).isEqualTo(fullSingleProductDefinition.report.id)
    assertThat(variant.name).isEqualTo(fullSingleProductDefinition.report.name)
    assertThat(variant.resourceName).isEqualTo("reports/${fullSingleProductDefinition.id}/${fullSingleProductDefinition.report.id}")
    assertThat(variant.description).isEqualTo(fullSingleProductDefinition.report.description)
    assertThat(variant.specification).isNotNull
    assertThat(variant.specification?.template).isEqualTo(fullSingleProductDefinition.report.specification?.template)
    assertThat(variant.specification?.fields).isNotEmpty
    assertThat(variant.specification?.fields).hasSize(1)

    val field = variant.specification!!.fields.first()
    val sourceSchemaField = fullSingleProductDefinition.dataset.schema.field.first()
    val sourceReportField = fullSingleProductDefinition.report.specification!!.field.first()

    assertThat(field.name).isEqualTo(sourceSchemaField.name)
    assertThat(field.display).isEqualTo(sourceReportField.display)
    assertThat(field.wordWrap.toString()).isEqualTo(sourceReportField.wordWrap.toString())
    assertThat(field.sortable).isEqualTo(sourceReportField.sortable)
    assertThat(field.defaultsort).isEqualTo(sourceReportField.defaultSort)
    assertThat(field.filter).isNotNull
    assertThat(field.filter?.type.toString()).isEqualTo(sourceReportField.filter?.type.toString())
    assertThat(field.filter?.staticOptions).isNotEmpty
    assertThat(field.filter?.staticOptions).hasSize(2)
    assertThat(field.filter?.staticOptions).isEqualTo(
      listOf(
        FilterOption("static1", "static1"),
        FilterOption("static2", "static2"),
      ),
    )
    assertThat(field.type.toString()).isEqualTo(sourceSchemaField.type.toString())
  }

  @Test
  fun `getting single report with dynamic options maps full data correctly and generates dynamic options in the result when returnAsStaticOptions is false`() {
    val reportWithDynamicFilter = Report(
      id = "21",
      name = "22",
      description = "23",
      created = LocalDateTime.MAX,
      version = "24",
      dataset = "\$ref:10",
      render = RenderMethod.PDF,
      schedule = "26",
      specification = Specification(
        template = "27",
        field = listOf(
          ReportField(
            name = "\$ref:13",
            display = "14",
            wordWrap = WordWrap.None,
            filter = FilterDefinition(
              type = FilterType.AutoComplete,
              dynamicOptions = DynamicFilterOption(minimumLength = 2, returnAsStaticOptions = false),
            ),
            sortable = true,
            defaultSort = true,
            formula = null,
            visible = true,
          ),
        ),
      ),
      destination = listOf(singletonMap("28", "29")),
      classification = "someClassification",
    )

    val fullSingleProductDefinition = fullSingleReportProductDefinition.copy(report = reportWithDynamicFilter)

    val mapper = ReportDefinitionMapper(configuredApiService)

    whenever(
      configuredApiService.validateAndFetchData(
        fullSingleProductDefinition.id,
        reportWithDynamicFilter.id, emptyMap(), 1, 10, "13", true, authToken, "13",
      ),
    ).thenReturn(listOf(mapOf("13" to "static1"), mapOf("13" to "static2")))

    val result = mapper.map(fullSingleProductDefinition, authToken)

    assertThat(result).isNotNull
    assertThat(result.id).isEqualTo(fullSingleProductDefinition.id)
    assertThat(result.name).isEqualTo(fullSingleProductDefinition.name)
    assertThat(result.description).isEqualTo(fullSingleProductDefinition.description)

    val variant = result.variant

    assertThat(variant.id).isEqualTo(fullSingleProductDefinition.report.id)
    assertThat(variant.name).isEqualTo(fullSingleProductDefinition.report.name)
    assertThat(variant.resourceName).isEqualTo("reports/${fullSingleProductDefinition.id}/${fullSingleProductDefinition.report.id}")
    assertThat(variant.description).isEqualTo(fullSingleProductDefinition.report.description)
    assertThat(variant.specification).isNotNull
    assertThat(variant.specification?.template).isEqualTo(fullSingleProductDefinition.report.specification?.template)
    assertThat(variant.specification?.fields).isNotEmpty
    assertThat(variant.specification?.fields).hasSize(1)

    val field = variant.specification!!.fields.first()
    val sourceSchemaField = fullSingleProductDefinition.dataset.schema.field.first()
    val sourceReportField = fullSingleProductDefinition.report.specification!!.field.first()

    assertThat(field.name).isEqualTo(sourceSchemaField.name)
    assertThat(field.display).isEqualTo(sourceReportField.display)
    assertThat(field.wordWrap.toString()).isEqualTo(sourceReportField.wordWrap.toString())
    assertThat(field.sortable).isEqualTo(sourceReportField.sortable)
    assertThat(field.defaultsort).isEqualTo(sourceReportField.defaultSort)
    assertThat(field.filter).isNotNull
    assertThat(field.filter?.type.toString()).isEqualTo(sourceReportField.filter?.type.toString())

    assertThat(field.filter?.staticOptions).isNull()
    assertThat(field.filter?.dynamicOptions).isEqualTo(DynamicFilterOption(2, false))
    assertThat(field.type.toString()).isEqualTo(sourceSchemaField.type.toString())
    verifyNoInteractions(configuredApiService)
  }

  private fun getExpectedDate(offset: Long, magnitude: ChronoUnit): String? {
    val expectedDate = when (magnitude) {
      ChronoUnit.DAYS -> LocalDate.now().plusDays(offset)
      ChronoUnit.WEEKS -> LocalDate.now().plusWeeks(offset)
      ChronoUnit.MONTHS -> LocalDate.now().plusMonths(offset)
      ChronoUnit.YEARS -> LocalDate.now().plusYears(offset)
      else -> null
    }.let { it!!.format(DateTimeFormatter.ISO_LOCAL_DATE) }
    return expectedDate
  }

  private fun createProductDefinitionWithDefaultFilter(defaultFilterValue: String): ProductDefinition {
    return ProductDefinition(
      id = "1",
      name = "2",
      metadata = MetaData(
        author = "3",
        owner = "4",
        version = "5",
      ),
      dataset = listOf(
        Dataset(
          id = "10",
          name = "11",
          query = "12",
          schema = Schema(
            field = listOf(
              SchemaField(
                name = "13",
                type = ParameterType.Date,
              ),
            ),
          ),
        ),
      ),
      report = listOf(
        Report(
          id = "16",
          name = "17",
          created = LocalDateTime.MAX,
          version = "18",
          dataset = "\$ref:10",
          render = RenderMethod.HTML,
          specification = Specification(
            template = "19",
            field = listOf(
              ReportField(
                name = "\$ref:13",
                display = "20",
                filter = FilterDefinition(
                  type = FilterType.DateRange,
                  default = defaultFilterValue,
                ),
                formula = null,
                visible = true,
              ),
            ),
          ),
          classification = "someClassification",
        ),
      ),
    )
  }
}
