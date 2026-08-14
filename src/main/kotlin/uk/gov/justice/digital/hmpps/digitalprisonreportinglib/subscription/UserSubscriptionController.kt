package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.getUserContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.AnySubscribableRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.UserSubscriptionRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.ManageUsersClient

@Validated
@RestController
@Tag(name = "User Subscription API")
class UserSubscriptionController(
  val userSubscriptionService: UserSubscriptionService,
  val manageUsersClient: ManageUsersClient,
  @Value("\${dpr.lib.hasProbationDatasources}")
  val hasProbationDatasources: Boolean,
) {

  @PostMapping("/user/subscribe")
  @Operation(
    description = "Subscribe user to report",
    security = [ SecurityRequirement(name = "bearer-jwt")],
  )
  fun subscribe(
    @RequestBody body: AnySubscribableRequest,
    httpRequest: HttpServletRequest,
  ) = userSubscriptionService.subscribe(
    UserSubscriptionRequest(
      httpRequest.getUserContext(manageUsersClient, hasProbationDatasources).userInfo.username,
      reportId = body.reportId,
      reportVariantId = body.reportVariantId,
    ),
  )

  @PostMapping("/user/unsubscribe")
  @Operation(
    description = "Unsubscribe user to report",
    security = [ SecurityRequirement(name = "bearer-jwt")],
  )
  fun unsubscribe(
    @RequestBody body: AnySubscribableRequest,
    httpRequest: HttpServletRequest,
  ) = userSubscriptionService.unsubscribe(
    UserSubscriptionRequest(
      httpRequest.getUserContext(manageUsersClient, hasProbationDatasources).userInfo.username,
      reportId = body.reportId,
      reportVariantId = body.reportVariantId,
    ),
  )

  @GetMapping("/user/subscriptions")
  @Operation(
    description = "User Subscriptions",
    security = [ SecurityRequirement(name = "bearer-jwt")],
  )
  fun subscriptions(
    httpRequest: HttpServletRequest,
  ) = userSubscriptionService.findByUserId(
    httpRequest.getUserContext(manageUsersClient, hasProbationDatasources).userInfo.username,
  )
}
