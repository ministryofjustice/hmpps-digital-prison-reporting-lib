package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ReportChild(
  val reportId: String,
  val joinField: List<String>,
)
