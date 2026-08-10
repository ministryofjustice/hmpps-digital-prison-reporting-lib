package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription

import java.time.LocalDateTime

enum class UserSubscriptionStatus {
  SUBSCRIBED,
  UNSUBSCRIBED,
}

data class UserSubscription(
  val id: String,
  val userId: String,
  val reportId: String,
  val reportVariantId: String,
  val status: String,
  val createdTime: LocalDateTime,
  val updatedTime: LocalDateTime? = null,
)

data class UserReportSubscription(
  val userId: String,
  val reportId: String,
  val reportVariantId: String,
  val tableId: String,
  val reportStatus: String,
  val reportUpdatedTime: LocalDateTime? = null,
)
