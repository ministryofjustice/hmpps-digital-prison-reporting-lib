package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.DataDefinitionPath
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy

interface AnyProductDefinition : WithPolicy {
  val id: String
  val name: String
  val description: String? get() = null
  val scheduled: Boolean? get() = false
  val metadata: MetaData

  val datasource: List<Datasource> get() = emptyList()
  val dataset: List<Dataset> get() = emptyList()
  var path: DataDefinitionPath?
  val report: List<AnyReport> get() = emptyList()
  override val policy: List<Policy> get() = emptyList()
  val dashboard: List<Dashboard>? get() = null
}

data class ProductDefinition(
  override val id: String,
  override val name: String,
  override val description: String? = null,
  override val scheduled: Boolean? = false,
  override val metadata: MetaData,
  override var path: DataDefinitionPath? = DataDefinitionPath.ORPHANAGE,
  override val datasource: List<Datasource> = emptyList(),
  override val dataset: List<Dataset> = emptyList(),
  override val report: List<Report> = emptyList(),
  override val policy: List<Policy> = emptyList(),
  override val dashboard: List<Dashboard>? = null,
) : WithPolicy,
  AnyProductDefinition

data class ProductDefinitionSummary(
  override val id: String,
  override val name: String,
  override val description: String? = null,
  override val metadata: MetaData,
  override var path: DataDefinitionPath? = DataDefinitionPath.ORPHANAGE,
  override val dataset: List<Dataset> = emptyList(),
  override val report: List<ReportLite> = emptyList(),
) : AnyProductDefinition
