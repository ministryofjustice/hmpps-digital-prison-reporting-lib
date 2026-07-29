package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.UserSubscriptionRequest

class UserSubscriptionService (
  private val userSubscriptionRepository: UserSubscriptionRepository
) {

  fun subscribe(request: UserSubscriptionRequest) {
    //TODO CHECK IF IT EXISTS FIRST ?
    val userSubscription = UserSubscription(
      userId = request.userId,
      reportId = request.reportId,
      reportVariantId = request.reportVariantId,
      status = UserSubscriptionStatus.SUBSCRIBED
    )
    userSubscriptionRepository.save(userSubscription)
    return userSubscription
  }: UserSubscription?

  fun unsubscribe(request: UserSubscriptionRequest) {

    userSubscriptionRepository.findByUserIdAndReport(request.userId, request.reportId, request.reportVariantId)

    }
  }
}