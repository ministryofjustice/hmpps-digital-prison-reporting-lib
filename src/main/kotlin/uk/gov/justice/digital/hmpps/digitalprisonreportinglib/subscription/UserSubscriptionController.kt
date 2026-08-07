package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.getUserContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.AnySubscribableRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.UserSubscriptionRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.ManageUsersClient

@Validated
@ConditionalOnProperty("spring.datasource.usersubscription.url")
@RestController
@Tag(name = "User Subscription API")
class UserSubscriptionController(
  val userSubscriptionService: UserSubscriptionService,
  val manageUsersClient: ManageUsersClient,
) {
  @ConditionalOnBean(UserSubscriptionService::class)
  @PostMapping("/user/subscribe")
  @Operation(
    description = "Subscribe user to report",
    security = [ SecurityRequirement(name = "bearer-jwt")],
  )
  fun subscribe(
    @RequestBody body: AnySubscribableRequest,
    httpRequest: HttpServletRequest,
  ) {
    userSubscriptionService.subscribe(
      UserSubscriptionRequest(
        httpRequest.getUserContext(manageUsersClient, false).userInfo.username,
        reportId = body.reportId,
        reportVariantId = body.reportVariantId,
      ),
    )
  }

  @ConditionalOnBean(UserSubscriptionService::class)
  @PostMapping("/user/unsubscribe")
  @Operation(
    description = "Unsubscribe user to report",
    security = [ SecurityRequirement(name = "bearer-jwt")],
  )
  fun unsubscribe(
    @RequestBody body: AnySubscribableRequest,
    httpRequest: HttpServletRequest,
  ) {
    userSubscriptionService.unsubscribe(
      UserSubscriptionRequest(
        httpRequest.getUserContext(manageUsersClient, false).userInfo.username,
        reportId = body.reportId,
        reportVariantId = body.reportVariantId,
      ),
    )
  }
}
