package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.DataDefinitionPath
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionParser.Companion.SCHEMA_VERSION_FIELD
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.WithPolicy
import kotlin.String

interface AnyProductDefinition : WithPolicy {
  val id: String
  val name: String
  val description: String? get() = null
  val scheduled: Boolean? get() = false
  val metadata: MetaData

  val datasource: List<Datasource> get() = emptyList()
  val dataset: List<Dataset> get() = emptyList()

  // path is deprecated - to be removed
  var path: DataDefinitionPath?
  val report: List<AnyReport> get() = emptyList()
  override val policy: List<Policy> get() = emptyList()
  val dashboard: List<Dashboard>? get() = null
}

@Serializable
data class ProductDefinition(
  override val id: String,
  override val name: String,
  override val description: String? = null,
  @SerialName(SCHEMA_VERSION_FIELD)
  @SerializedName(SCHEMA_VERSION_FIELD)
  val schemaVersion: String? = null,
  override val scheduled: Boolean? = false,
  override val metadata: MetaData,
  // path is deprecated - to be removed
  override var path: DataDefinitionPath? = DataDefinitionPath.ORPHANAGE,
  override val datasource: List<Datasource>,
  override val dataset: List<Dataset>,
  override val report: List<Report> = emptyList(),
  override val policy: List<Policy>,
  override val dashboard: List<Dashboard>? = null,
) : AnyProductDefinition {

  fun toSummary(): ProductDefinitionSummary = ProductDefinitionSummary(
    id = id,
    name = name,
    description = description,
    metadata = metadata,
    path = path,
    dataset = dataset,
    report = report.map { it.toReportLite() },
    policy = policy,
    dashboard = dashboard,
  )
}

@Serializable
data class ProductDefinitionSummary(
  override val id: String,
  override val name: String,
  override val description: String? = null,
  override val metadata: MetaData,
  // path is deprecated - to be removed
  override var path: DataDefinitionPath? = DataDefinitionPath.ORPHANAGE,
  override val dataset: List<Dataset>,
  override val report: List<ReportLite> = emptyList(),
  override val policy: List<Policy>,
  override val dashboard: List<Dashboard>? = null,
) : AnyProductDefinition
