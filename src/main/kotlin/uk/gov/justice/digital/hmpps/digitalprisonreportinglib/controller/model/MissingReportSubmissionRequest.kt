package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class MissingReportSubmissionRequest(
  val userId: String,
  val reportId: String,
  val reportVariantId: String,
  val reason: String?,
)
