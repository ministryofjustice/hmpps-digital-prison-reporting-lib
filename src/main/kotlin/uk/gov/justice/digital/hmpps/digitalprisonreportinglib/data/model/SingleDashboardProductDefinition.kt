package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy

data class SingleDashboardProductDefinition(
  val id: String,
  val name: String,
  val description: String? = null,
  val metadata: MetaData,
  val datasource: Datasource,
  val policy: List<Policy>,
  val dashboardDataset: Dataset,
  val dashboard: Dashboard,
)
