package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy

data class SingleReportProductDefinition(
  val id: String,
  val name: String,
  val description: String? = null,
  val metadata: MetaData,
  val datasource: Datasource,
  val dataset: Dataset,
  val report: Report,
  val policy: List<Policy>,
)
