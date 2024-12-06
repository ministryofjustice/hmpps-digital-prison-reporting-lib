package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy

data class SingleDashboardProductDefinition(
  val id: String,
  val name: String,
  val description: String? = null,
  val metadata: MetaData,
  val datasource: Datasource,
  override val policy: List<Policy>,
  val dashboardDataset: Dataset,
  val dashboard: Dashboard,
  val allDatasets: List<Dataset>,
) : WithPolicy
