package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.common.model.LoadType
import java.time.LocalDateTime

interface AnyReport: Identified {
  val id: String
  val name: String
  val description: String?
  val dataset: String
  val render: RenderMethod
  val loadType: LoadType? get() = null
  override fun getIdentifier() = id
}

data class ReportLite(
  override val id: String,
  override val name: String,
  override val description: String? = null,
  override val dataset: String,
  override val render: RenderMethod,
) : AnyReport

data class Report(
  override val id: String,
  override val name: String,
  override val description: String? = null,
  val created: LocalDateTime?,
  val version: String,
  override val dataset: String,
  override val render: RenderMethod,
  val schedule: String? = null,
  val specification: Specification? = null,
  val destination: List<Map<String, String>> = emptyList(),
  val classification: String? = null,
  val feature: List<Feature>? = emptyList(),
  val summary: List<ReportSummary>? = emptyList(),
  val filter: ReportFilter? = null,
  val metadata: ReportMetadata? = null,
  val child: List<ReportChild>? = null,
  val isMissing: Boolean = false,
  override val loadType: LoadType? = null,
) : AnyReport
