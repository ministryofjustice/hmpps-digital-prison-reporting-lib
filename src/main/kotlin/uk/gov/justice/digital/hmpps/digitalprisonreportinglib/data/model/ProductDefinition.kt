package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy

data class ProductDefinition(
  val id: String,
  val name: String,
  val description: String? = null,
  val metadata: MetaData,
  val datasource: List<Datasource> = emptyList(),
  val dataset: List<Dataset> = emptyList(),
  val report: List<Report> = emptyList(),
  val policy: List<Policy> = emptyList(),
  val dashboards: List<Dashboard>? = null,
  val metrics: List<Metric>? = null,
)
