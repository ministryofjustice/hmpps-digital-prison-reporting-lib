package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class DashboardOption(
  val useRagColour: Boolean? = null,
  val baseColour: String? = null,
  val bucket: List<DashboardBucket>? = null,
  val showLatest: Boolean? = null,
  val columnsAsList: Boolean? = null,
)
