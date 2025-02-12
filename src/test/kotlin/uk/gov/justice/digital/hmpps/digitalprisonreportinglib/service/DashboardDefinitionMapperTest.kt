package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardSectionDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationColumnDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationColumnsDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DashboardVisualisationTypeDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.DynamicFilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FieldType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IdentifiedHelper
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.IsoLocalDateTimeTypeAdaptor
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.JsonFileProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.establishmentsAndWings.EstablishmentToWing
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dashboard
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Parameter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReferenceType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Schema
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.estcodesandwings.EstablishmentCodesToWingsCacheService

class DashboardDefinitionMapperTest {

  private val productDefinitionRepository: ProductDefinitionRepository = JsonFileProductDefinitionRepository(
    listOf("productDefinitionWithMetrics.json"),
    DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
    identifiedHelper = IdentifiedHelper(),
  )
  private val syncDataApiService: SyncDataApiService = Mockito.mock()
  private val establishmentCodesToWingsCacheService: EstablishmentCodesToWingsCacheService = Mockito.mock()

  private val dashboardDefinitionMapper = DashboardDefinitionMapper(syncDataApiService, IdentifiedHelper(), establishmentCodesToWingsCacheService)

  @Test
  fun `getDashboardDefinition returns the dashboard definition`() {
    whenever(syncDataApiService.validateAndFetchDataForFilterWithDataset(any(), any(), any())).then {
      listOf(
        mapOf("establishment_id" to "AAA", "establishment_name" to "Aardvark"),
        mapOf("establishment_id" to "BBB", "establishment_name" to "Bumblebee"),
      )
    }

    val productDefinition = productDefinitionRepository.getSingleDashboardProductDefinition("missing-ethnicity-metrics", "age-breakdown-dashboard-1")

    val actual = dashboardDefinitionMapper.toDashboardDefinition(
      dashboard = productDefinition.dashboard,
      allDatasets = productDefinition.allDatasets,
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
                    DashboardVisualisationColumnDefinition(id = "establishment_id", display = "Establishmnent ID"),
                    DashboardVisualisationColumnDefinition(id = "wing", display = "Wing"),
                  ),
                  measures = listOf(
                    DashboardVisualisationColumnDefinition(id = "establishment_id", display = "Establishmnent ID"),
                    DashboardVisualisationColumnDefinition(id = "wing", display = "Wing"),
                    DashboardVisualisationColumnDefinition(id = "total_prisoners", display = "Total prisoners"),
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
      uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType.AutoComplete,
      "display",
      true,
      ReferenceType.ESTABLISHMENT,
    )
    val dashboardDataset = Dataset(
      datasetId,
      "name",
      "datasource",
      "query",
      Schema(listOf(SchemaField("n", ParameterType.String, "d"))),
      listOf(
        parameter,
      ),
    )
    val actual = dashboardDefinitionMapper.toDashboardDefinition(
      dashboard = dashboard,
      allDatasets = listOf(dashboardDataset),
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
          filter = FilterDefinition(
            type = FilterType.AutoComplete,
            mandatory = parameter.mandatory,
            interactive = false,
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
}
