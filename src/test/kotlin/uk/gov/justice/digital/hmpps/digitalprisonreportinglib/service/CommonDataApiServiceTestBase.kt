package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ParameterType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ReportField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Schema
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SchemaField
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Specification
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Template

open class CommonDataApiServiceTestBase {

  protected fun report(fields: List<ReportField>? = null): Report = Report(
    id = "reportId",
    name = "reportName",
    version = "1",
    render = RenderMethod.HTML,
    dataset = "10",
    specification =
    Specification(
      template = Template.List,
      field = fields ?: listOf(
        ReportField(name = "13", display = null),
      ),
      section = null,
    ),
    created = null,
  )

  protected fun dataset(schedule: String? = null, fields: List<SchemaField>? = null): Dataset = Dataset(
    id = "10",
    name = "11",
    datasource = "12A",
    query = "12",
    schema = Schema(
      field = fields
        ?: listOf(
          SchemaField(
            name = "13",
            type = ParameterType.Long,
            display = "14",
            filter = null,
          ),
        ),
    ),
    schedule = schedule,
  )
}
