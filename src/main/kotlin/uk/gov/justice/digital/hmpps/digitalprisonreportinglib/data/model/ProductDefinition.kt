package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.DataDefinitionPath
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.LoadType
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy

data class ProductDefinition(
  val id: String,
  val name: String,
  val description: String? = null,
  val scheduled: Boolean? = false,
  val metadata: MetaData,
  var path: DataDefinitionPath? = DataDefinitionPath.ORPHANAGE,
  val datasource: List<Datasource> = emptyList(),
  val dataset: List<Dataset> = emptyList(),
  val report: List<Report> = emptyList(),
  override val policy: List<Policy> = emptyList(),
  val dashboard: List<Dashboard>? = null,
) : WithPolicy
