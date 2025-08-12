package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.missingReport

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MissingReportSubmissionRequest
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.DprAuthAwareAuthenticationToken

@Validated
@ConditionalOnProperty("spring.datasource.missingreport.url")
@RestController
@Tag(name = "Missing report submission API")
class MissingReportSubmissionController(
  val missingReportService: MissingReportService,
) {

  @ConditionalOnBean(MissingReportService::class)
  @PostMapping("/definitions/{reportId}/{variantId}/missingRequest")
  @Operation(
    description = "Submit a request for a missing report",
    security = [ SecurityRequirement(name = "bearer-jwt")],
  )
  fun requestMissing(
    @PathVariable("reportId")
    reportId: String,
    @Parameter(
      description = "The ID of the variant definition.",
      example = "list",
    )
    @PathVariable("variantId")
    variantId: String,
    @RequestBody body: String?,
    authentication: Authentication,
  ): MissingReportSubmission {
    return missingReportService.createMissingReportSubmission(
      MissingReportSubmissionRequest(
        (authentication as DprAuthAwareAuthenticationToken).getUsername(),
        reportId,
        variantId,
        body,
      ),
    )
  }
}