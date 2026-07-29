package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

data class AnySubscribableRequest(
  val reportId: String,
  val reportVariantId: String,
)

data class UserSubscriptionRequest (
  val userId: String,
  val reportId: String,
  val reportVariantId: String,
)