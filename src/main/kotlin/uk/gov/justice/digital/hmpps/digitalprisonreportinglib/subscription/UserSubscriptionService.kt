package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.UserSubscriptionRequest
import java.time.LocalDateTime
import java.util.UUID

class UserSubscriptionService(
  private val userSubscriptionRepository: UserSubscriptionRepository,
) {

  fun subscribe(request: UserSubscriptionRequest): UserSubscription {
    val userSubscription = UserSubscription(
      id = UUID.randomUUID().toString(),
      userId = request.userId,
      reportId = request.reportId,
      reportVariantId = request.reportVariantId,
      status = UserSubscriptionStatus.SUBSCRIBED.name,
      createdTime = LocalDateTime.now(),
    )
    return userSubscriptionRepository.create(userSubscription)!!
  }

  fun unsubscribe(request: UserSubscriptionRequest): UserSubscription? = userSubscriptionRepository.findByUserIdAndReport(
    request.userId,
    request.reportId,
    request.reportVariantId,
  )?.let {
    userSubscriptionRepository.updateSubscription(
      it.copy(
        status = UserSubscriptionStatus.UNSUBSCRIBED.name,
        updatedTime = LocalDateTime.now(),
      ),
    )
  }

  fun findByUserId(userId: String): List<UserReportSubscription> = userSubscriptionRepository.findByUserId(userId).map { userSubscription ->
    UserReportSubscription(
      userId = userSubscription.userId,
      reportId = userSubscription.reportId,
      reportVariantId = userSubscription.reportVariantId,
      tableId = "",
      reportStatus = userSubscription.status,
    )
  }
}
