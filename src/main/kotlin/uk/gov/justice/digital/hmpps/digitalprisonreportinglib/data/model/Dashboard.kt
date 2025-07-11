package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.LoadType

data class Dashboard(
  val id: String,
  val name: String,
  val description: String? = null,
  val dataset: String,
  val section: List<DashboardSection>,
  val filter: ReportFilter? = null,
  val loadType: LoadType? = null,
)
