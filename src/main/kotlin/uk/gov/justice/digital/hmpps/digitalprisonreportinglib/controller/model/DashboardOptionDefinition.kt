package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class DashboardOptionDefinition(
  val useRagColour: Boolean? = null,
  val baseColour: String? = null,
  val buckets: List<DashboardBucketDefinition>? = null,
  val showLatest: Boolean? = null,
  val columnsAsList: Boolean? = null,
)
