package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.DataSet
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.DataSource
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.FilterDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.FilterOption
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.FilterType
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.MetaData
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.Schema
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.Specification
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.WordWrap
import java.time.LocalDate

@Service
class StubbedProductDefinitionRepository : ProductDefinitionRepository {

  override fun getProductDefinitions(): List<ProductDefinition> {
    val fields = listOf(
      SchemaField(
        name = "prisonNumber",
        type = ParameterType.String,
      ),
      SchemaField(
        name = "name",
        type = ParameterType.String,
      ),
      SchemaField(
        name = "date",
        type = ParameterType.Date,
      ),
      SchemaField(
        name = "origin",
        type = ParameterType.String,
      ),
      SchemaField(
        name = "destination",
        type = ParameterType.String,
      ),
      SchemaField(
        name = "direction",
        type = ParameterType.String,
      ),
      SchemaField(
        name = "type",
        type = ParameterType.String,
      ),
      SchemaField(
        name = "reason",
        type = ParameterType.String,
      ),
    )

    return listOf(
      ProductDefinition(
        id = "external-movements",
        name = "External Movements",
        description = "Reports about prisoner external movements",
        metaData = MetaData(author = "Adam", version = "1.2.3", owner = "Eve"),
        dataSet = listOf(
          DataSet(
            id = "external-movements",
            name = "All movements",
            query = "SELECT " +
              "prisoners.number AS prisonNumber," +
              "CONCAT(CONCAT(prisoners.lastname, ', '), substring(prisoners.firstname, 1, 1)) AS name," +
              "movements.date," +
              "movements.direction," +
              "movements.type," +
              "movements.origin," +
              "movements.destination," +
              "movements.reason\n" +
              "FROM datamart.domain.movements_movements as movements\n" +
              "JOIN datamart.domain.prisoner_prisoner as prisoners\n" +
              "ON movements.prisoner = prisoners.id",
            schema = Schema(
              field = fields,
            ),
          ),
        ),
        dataSource = listOf(
          DataSource(
            id = "redshift",
            name = "RedShift",
            connection = "redshift",
          ),
        ),
        report = listOf(
          Report(
            id = "last-month",
            name = "Last month",
            description = "All movements in the past month",
            dataset = "\$ref:external-movements",
            policy = emptyList(),
            specification = Specification(
              template = "list",
              field = listOf(
                ReportField(
                  schemaField = "\$ref:prisonNumber",
                  displayName = "Prison Number",
                ),
                ReportField(
                  schemaField = "\$ref:name",
                  displayName = "Name",
                  wordWrap = WordWrap.None,
                ),
                ReportField(
                  schemaField = "\$ref:date",
                  displayName = "Date",
                  defaultSortColumn = true,
                  filter = FilterDefinition(
                    type = FilterType.DateRange,
                    defaultValue = "today(-1,months) - today()",
                  ),
                ),
                ReportField(
                  schemaField = "\$ref:origin",
                  displayName = "From",
                  wordWrap = WordWrap.None,
                ),
                ReportField(
                  schemaField = "\$ref:destination",
                  displayName = "To",
                  wordWrap = WordWrap.None,
                ),
                ReportField(
                  schemaField = "\$ref:direction",
                  displayName = "Direction",
                  filter = FilterDefinition(
                    type = FilterType.Radio,
                    staticOptions = listOf(
                      FilterOption("in", "In"),
                      FilterOption("out", "Out"),
                    ),
                  ),
                ),
                ReportField(
                  schemaField = "\$ref:type",
                  displayName = "Type",
                ),
                ReportField(
                  schemaField = "\$ref:reason",
                  displayName = "Reason",
                ),
              ),
            ),
            render = RenderMethod.HTML,
            created = LocalDate.now(),
            version = "1.2.3",
          ),
          Report(
            id = "last-week",
            name = "Last week",
            description = "All movements in the past week",
            dataset = "\$ref:external-movements",
            policy = emptyList(),
            specification = Specification(
              template = "list",
              field = listOf(
                ReportField(
                  schemaField = "\$ref:prisonNumber",
                  displayName = "Prison Number",
                ),
                ReportField(
                  schemaField = "\$ref:name",
                  displayName = "Name",
                  wordWrap = WordWrap.None,
                ),
                ReportField(
                  schemaField = "\$ref:date",
                  displayName = "Date",
                  defaultSortColumn = true,
                  filter = FilterDefinition(
                    type = FilterType.DateRange,
                    defaultValue = "today(-1,weeks) - today()",
                  ),
                ),
                ReportField(
                  schemaField = "\$ref:origin",
                  displayName = "From",
                  wordWrap = WordWrap.None,
                ),
                ReportField(
                  schemaField = "\$ref:destination",
                  displayName = "To",
                  wordWrap = WordWrap.None,
                ),
                ReportField(
                  schemaField = "\$ref:direction",
                  displayName = "Direction",
                  filter = FilterDefinition(
                    type = FilterType.Radio,
                    staticOptions = listOf(
                      FilterOption("in", "In"),
                      FilterOption("out", "Out"),
                    ),
                  ),
                ),
                ReportField(
                  schemaField = "\$ref:type",
                  displayName = "Type",
                ),
                ReportField(
                  schemaField = "\$ref:reason",
                  displayName = "Reason",
                ),
              ),
            ),
            render = RenderMethod.HTML,
            created = LocalDate.now(),
            version = "1.2.3",
          ),
        ),
      ),
    )
  }
}
