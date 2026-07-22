package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardSectionDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationColumnDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationColumnsDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DynamicFilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldSource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ValueVisualisationColumnDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IsoLocalDateTimeTypeAdaptor
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.JsonFileProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.QueryDeserializer.Companion.PLACEHOLDER_DATASOURCE
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings.EstablishmentToWing
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dashboard
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType.AutoComplete
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType.Caseloads
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MultiphaseQuery
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Parameter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReferenceType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Schema
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.CaseloadResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication.AuthUser
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.alert.AlertCategoryCacheService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.estcodesandwings.EstablishmentCodesToWingsCacheService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
import uk.gov.justice.hmpps.kotlin.auth.AuthSource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterDefinition as DataFilterDefinition

class DashboardDefinitionMapperTest {

  private val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
    listOf("productDefinitionWithDashboard.json"),
    DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
    identifiedHelper = IdentifiedHelper(),
  )
  private val syncDataApiService: SyncDataApiService = mock()
  private val establishmentCodesToWingsCacheService: EstablishmentCodesToWingsCacheService = mock()
  private val alertCategoryCacheService: AlertCategoryCacheService = mock()
  private val productDefinitionTokenPolicyChecker: ProductDefinitionTokenPolicyChecker = mock()

  private val dashboardDefinitionMapper = DashboardDefinitionMapper(
    syncDataApiService = syncDataApiService,
    identifiedHelper = IdentifiedHelper(),
    establishmentCodesToWingsCacheService = establishmentCodesToWingsCacheService,
    alertCategoryCacheService = alertCategoryCacheService,
    productDefinitionRepository = productDefinitionRepository,
    productDefinitionTokenPolicyChecker = productDefinitionTokenPolicyChecker,
  )

  private val executionContext = ExecutionContext(
    CaseloadResponse(
      username = "request-user",
      active = true,
      accountType = "GENERAL",
      caseloads = listOf(
        Caseload("KMI", "KIRKHAM"),
        Caseload("WWI", "WANDSWORTH (HMP)"),
      ),
      activeCaseload = Caseload(id = "WWI", name = "WANDSWORTH (HMP)"),
    ),
    listOf("ROLE_PRISONS_REPORTING_USER"),
    AuthUser("request-user", true, "request-user", AuthSource.NOMIS, "abc123", "f23-f2-f32f23-f3223f"),
    false,
  )

  @Test
  fun `getDashboardDefinition returns the dashboard definition`() {
    whenever(syncDataApiService.validateAndFetchDataForFilterWithDataset(any(), any(), any(), anyOrNull())).then {
      listOf(
        mapOf("establishment_id" to "AAA", "establishment_name" to "Aardvark"),
        mapOf("establishment_id" to "BBB", "establishment_name" to "Bumblebee"),
      )
    }

    val productDefinition = productDefinitionRepository.getSingleDashboardProductDefinition("missing-ethnicity-metrics", "age-breakdown-dashboard-1")
    val actual = dashboardDefinitionMapper.toDashboardDefinition(
      dashboard = productDefinition.dashboard,
      allDashboards = productDefinition.allDashboards,
      allDatasets = productDefinition.allDatasets,
      executionContext = executionContext,
    )
    assertEquals(
      DashboardDefinition(
        id = "age-breakdown-dashboard-1",
        name = "Age Breakdown Dashboard",
        description = "Age Breakdown Dashboard Description",
        sections = listOf(
          DashboardSectionDefinition(
            id = "totals-breakdown",
            display = "Totals breakdown",
            visualisations = listOf(
              DashboardVisualisationDefinition(
                id = "total-prisoners",
                type = DashboardVisualisationTypeDefinition.LIST,
                display = "Total prisoners by wing",
                columns = DashboardVisualisationColumnsDefinition(
                  keys = listOf(
                    DashboardVisualisationColumnDefinition(id = "establishment_id", display = "Establishment ID", type = FieldType.HTML),
                    DashboardVisualisationColumnDefinition(id = "wing", display = "Wing"),
                  ),
                  measures = listOf(
                    DashboardVisualisationColumnDefinition(id = "establishment_id", display = "Establishment ID", type = FieldType.HTML),
                    DashboardVisualisationColumnDefinition(id = "wing", display = "Wing"),
                    DashboardVisualisationColumnDefinition(id = "total_prisoners", display = "Total prisoners"),
                  ),
                  filters = listOf(
                    ValueVisualisationColumnDefinition(id = "establishment_id", equals = null),
                  ),
                  expectNulls = true,
                ),
              ),
            ),
          ),
        ),
        filterFields = listOf(
          FieldDefinition(
            name = "establishment_id",
            display = "Establishment ID",
            filter = FilterDefinition(
              type = FilterType.Select,
              mandatory = false,
              staticOptions = listOf(
                FilterOption(name = "AAA", display = "Aardvark"),
                FilterOption(name = "BBB", display = "Bumblebee"),
              ),
              dynamicOptions = DynamicFilterOption(minimumLength = null),
              interactive = true,
            ),
            sortable = true,
            defaultsort = false,
            type = FieldType.String,
            mandatory = false,
            visible = true,
            calculated = false,
            header = false,
          ),
        ),
      ),
      actual,
    )

    verify(syncDataApiService).validateAndFetchDataForFilterWithDataset(
      pageSize = eq(123L),
      sortColumn = eq("establishment_id"),
      dataset = any(),
      prompts = anyOrNull(),
    )
  }

  @Test
  fun `getDashboardDefinition converts dataset parameters to filters and returns the dashboard definition`() {
    whenever(establishmentCodesToWingsCacheService.getEstablishmentsAndPopulateCacheIfNeeded()).then {
      mapOf("KMI" to listOf(EstablishmentToWing("KMI", "KIRKHAM", "A")))
    }

    val datasetId = "dataset-id"
    val id = "age-breakdown-dashboard-1"
    val name = "test-dashboard-name"
    val description = "description"
    val dashboard = Dashboard(
      id,
      name,
      description,
      datasetId,
      listOf(),
    )
    val parameter = Parameter(
      0,
      "paramName",
      ParameterType.String,
      AutoComplete,
      "display",
      true,
      ReferenceType.ESTABLISHMENT,
    )
    val dashboardDataset = Dataset(
      datasetId,
      "name",
      "datasource",
      listOf(MultiphaseQuery(index = 0, datasource = PLACEHOLDER_DATASOURCE, query = "query")),
      Schema(listOf(SchemaField("n", ParameterType.String, "d"))),
      listOf(
        parameter,
      ),
    )
    val actual = dashboardDefinitionMapper.toDashboardDefinition(
      dashboard = dashboard,
      allDashboards = listOf(dashboard),
      allDatasets = listOf(dashboardDataset),
      executionContext = executionContext,
    )
    val expected = DashboardDefinition(
      id,
      name,
      description,
      listOf(),
      listOf(
        FieldDefinition(
          name = parameter.name,
          display = parameter.display,
          sortable = false,
          defaultsort = false,
          type = FieldType.String,
          mandatory = false,
          visible = false,
          fieldSource = FieldSource.ParamField,
          filter = FilterDefinition(
            type = FilterType.AutoComplete,
            mandatory = parameter.mandatory,
            interactive = false,
            index = parameter.index,
            staticOptions = listOf(
              FilterOption(
                "KMI",
                "KIRKHAM",
              ),
            ),
          ),
        ),
      ),
    )
    assertEquals(expected, actual)
    verify(establishmentCodesToWingsCacheService, times(1)).getEstablishmentsAndPopulateCacheIfNeeded()
  }

  @Test
  fun `getDashboardDefinition converts multiphase query dataset parameters to filters and returns the dashboard definition`() {
    whenever(establishmentCodesToWingsCacheService.getEstablishmentsAndPopulateCacheIfNeeded()).then {
      mapOf("KMI" to listOf(EstablishmentToWing("KMI", "KIRKHAM", "A")))
    }

    val datasetId = "dataset-id"
    val id = "age-breakdown-dashboard-1"
    val name = "test-dashboard-name"
    val description = "description"
    val dashboard = Dashboard(
      id,
      name,
      description,
      datasetId,
      listOf(),
    )
    val parameter = Parameter(
      0,
      "paramName",
      ParameterType.String,
      AutoComplete,
      "display",
      true,
      ReferenceType.ESTABLISHMENT,
    )
    // Parameters are taken from the MultiphaseQuery when there are at least 2 queries. Otherwise, the parameters are taken from the dataset parameters are originally for the single queries.
    val multiphaseQuery1 = MultiphaseQuery(
      index = 0,
      datasource = "ds1",
      query = "SELECT * FROM a",
      parameters = listOf(parameter),
    )
    val multiphaseQuery2 = MultiphaseQuery(
      index = 0,
      datasource = "ds1",
      query = "SELECT * FROM a",
    )
    val dashboardDataset = Dataset(
      id = datasetId,
      name = "name",
      datasource = "datasource",
      query = listOf(multiphaseQuery1, multiphaseQuery2),
      schema = Schema(listOf(SchemaField("n", ParameterType.String, "d"))),
    )
    val actual = dashboardDefinitionMapper.toDashboardDefinition(
      dashboard = dashboard,
      allDashboards = listOf(dashboard),
      allDatasets = listOf(dashboardDataset),
      executionContext = executionContext,
    )
    val expected = DashboardDefinition(
      id,
      name,
      description,
      listOf(),
      listOf(
        FieldDefinition(
          name = parameter.name,
          display = parameter.display,
          sortable = false,
          defaultsort = false,
          type = FieldType.String,
          mandatory = false,
          visible = false,
          fieldSource = FieldSource.ParamField,
          filter = FilterDefinition(
            type = FilterType.AutoComplete,
            mandatory = parameter.mandatory,
            interactive = false,
            index = parameter.index,
            staticOptions = listOf(
              FilterOption(
                "KMI",
                "KIRKHAM",
              ),
            ),
          ),
        ),
      ),
    )
    assertEquals(expected, actual)
    verify(establishmentCodesToWingsCacheService, times(1)).getEstablishmentsAndPopulateCacheIfNeeded()
  }

  @Test
  fun `getDashboardDefinition converts caseloads filter to multiselect filter and returns the dashboard definition`() {
    val datasetId = "dataset-id"
    val id = "age-breakdown-dashboard-1"
    val name = "test-dashboard-name"
    val description = "description"
    val dashboard = Dashboard(
      id,
      name,
      description,
      datasetId,
      listOf(),
    )
    val schemaField = SchemaField(
      name = "n",
      type = ParameterType.String,
      display = "d",
      filter = DataFilterDefinition(
        type = Caseloads,
      ),
    )
    val dashboardDataset = Dataset(
      id = datasetId,
      name = "name",
      datasource = "datasource",
      query = listOf(MultiphaseQuery(index = 0, datasource = PLACEHOLDER_DATASOURCE, query = "query")),
      schema = Schema(
        listOf(
          schemaField,
        ),
      ),
    )
    val actual = dashboardDefinitionMapper.toDashboardDefinition(
      dashboard = dashboard,
      allDashboards = listOf(dashboard),
      allDatasets = listOf(dashboardDataset),
      executionContext = executionContext,
    )
    val expected = DashboardDefinition(
      id,
      name,
      description,
      listOf(),
      listOf(
        FieldDefinition(
          name = "n",
          display = "d",
          sortable = true,
          defaultsort = false,
          type = FieldType.String,
          mandatory = false,
          visible = true,
          filter = FilterDefinition(
            defaultValue = "KMI,WWI",
            type = FilterType.Multiselect,
            mandatory = false,
            interactive = false,
            staticOptions = listOf(
              FilterOption(
                "KMI",
                "KIRKHAM",
              ),
              FilterOption(
                "WWI",
                "WANDSWORTH (HMP)",
              ),
            ),
          ),
        ),
      ),
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `getDashboardDefinition returns the dashboard definition for parent-child dashboards`() {
    whenever(syncDataApiService.validateAndFetchDataForFilterWithDataset(any(), any(), any(), anyOrNull())).then {
      listOf(
        mapOf("establishment_id" to "AAA", "establishment_name" to "Aardvark"),
        mapOf("establishment_id" to "BBB", "establishment_name" to "Bumblebee"),
      )
    }

    val productDefinitionRepositoryWithChild: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
      listOf("productDefinitionWithDashboardChild.json"),
      DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
      identifiedHelper = IdentifiedHelper(),
    )

    val dashboardDefinitionMapperWithChild = DashboardDefinitionMapper(
      syncDataApiService = syncDataApiService,
      identifiedHelper = IdentifiedHelper(),
      establishmentCodesToWingsCacheService = establishmentCodesToWingsCacheService,
      alertCategoryCacheService = alertCategoryCacheService,
      productDefinitionRepository = productDefinitionRepositoryWithChild,
      productDefinitionTokenPolicyChecker = productDefinitionTokenPolicyChecker,
    )

    val productDefinitionWithChild = productDefinitionRepositoryWithChild.getSingleDashboardProductDefinition("missing-ethnicity-metrics", "age-breakdown-dashboard-with-child")
    val actual = dashboardDefinitionMapperWithChild.toDashboardDefinition(
      dashboard = productDefinitionWithChild.dashboard,
      allDashboards = productDefinitionWithChild.allDashboards,
      allDatasets = productDefinitionWithChild.allDatasets,
      executionContext = executionContext,
    )

    val expectedSections = listOf(
      DashboardSectionDefinition(
        id = "totals-breakdown",
        display = "Totals breakdown",
        visualisations = listOf(
          DashboardVisualisationDefinition(
            id = "total-prisoners",
            type = DashboardVisualisationTypeDefinition.LIST,
            display = "Total prisoners by wing",
            columns = DashboardVisualisationColumnsDefinition(
              keys = listOf(
                DashboardVisualisationColumnDefinition(
                  id = "establishment_id",
                  display = "Establishment ID",
                  type = FieldType.HTML,
                ),
                DashboardVisualisationColumnDefinition(id = "wing", display = "Wing"),
              ),
              measures = listOf(
                DashboardVisualisationColumnDefinition(
                  id = "establishment_id",
                  display = "Establishment ID",
                  type = FieldType.HTML,
                ),
                DashboardVisualisationColumnDefinition(id = "wing", display = "Wing"),
                DashboardVisualisationColumnDefinition(id = "total_prisoners", display = "Total prisoners"),
              ),
              filters = listOf(
                ValueVisualisationColumnDefinition(id = "establishment_id", equals = null),
              ),
              expectNulls = true,
            ),
          ),
        ),
      ),
    )
    val expectedFilterFields = listOf(
      FieldDefinition(
        name = "establishment_id",
        display = "Establishment ID",
        filter = FilterDefinition(
          type = FilterType.Select,
          mandatory = false,
          staticOptions = listOf(
            FilterOption(name = "AAA", display = "Aardvark"),
            FilterOption(name = "BBB", display = "Bumblebee"),
          ),
          dynamicOptions = DynamicFilterOption(minimumLength = null),
          interactive = true,
        ),
        sortable = true,
        defaultsort = false,
        type = FieldType.String,
        mandatory = false,
        visible = true,
        calculated = false,
        header = false,
      ),
    )
    val expectedChildVariants = listOf(
      DashboardDefinition(
        id = "establishment-dashboard",
        name = "Establishment Dashboard",
        description = "Establishment Dashboard Description",
        sections = expectedSections,
        filterFields = expectedFilterFields,
        childVariants = null,
      ),
    )

    assertEquals(
      DashboardDefinition(
        id = "age-breakdown-dashboard-with-child",
        name = "Age Breakdown Dashboard",
        description = "Age Breakdown Dashboard Description",
        sections = expectedSections,
        filterFields = expectedFilterFields,
        childVariants = expectedChildVariants,
      ),
      actual,
    )

    verify(syncDataApiService, times(2)).validateAndFetchDataForFilterWithDataset(
      pageSize = eq(123L),
      sortColumn = eq("establishment_id"),
      dataset = any(),
      prompts = anyOrNull(),
    )
  }
}
