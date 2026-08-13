package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.UserSubscriptionRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.TableIdGenerator
import java.time.LocalDateTime
import java.util.UUID

class UserSubscriptionService(
  private val userSubscriptionRepository: UserSubscriptionRepository,
  private val productDefinitionRepository: ProductDefinitionRepository,
  private val tableIdGenerator: TableIdGenerator,
) {

  fun subscribe(request: UserSubscriptionRequest): UserSubscription {
    val productDefinition = productDefinitionRepository.getSingleReportProductDefinition(
      request.reportId,
      request.reportVariantId,
    )
    val tableId = tableIdGenerator.generateScheduledDatasetId(productDefinition)
    return userSubscriptionRepository.findByUserIdAndReport(
      request.userId,
      request.reportId,
      request.reportVariantId,
    )?.let { existing ->
      if (existing.status == UserSubscriptionStatus.SUBSCRIBED.name) {
        existing
      } else {
        userSubscriptionRepository.updateSubscription(
          existing.copy(
            status = UserSubscriptionStatus.SUBSCRIBED.name,
            updatedTime = LocalDateTime.now(),
          ),
        )
      }
    } ?: userSubscriptionRepository.create(
      UserSubscription(
        id = UUID.randomUUID().toString(),
        userId = request.userId,
        reportId = request.reportId,
        reportVariantId = request.reportVariantId,
        tableId = tableId,
        status = UserSubscriptionStatus.SUBSCRIBED.name,
        createdTime = LocalDateTime.now(),
      ),
    )!!
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

  fun findByUserId(userId: String): List<UserReportSubscription> = userSubscriptionRepository.findReportsByUserId(userId)
}
