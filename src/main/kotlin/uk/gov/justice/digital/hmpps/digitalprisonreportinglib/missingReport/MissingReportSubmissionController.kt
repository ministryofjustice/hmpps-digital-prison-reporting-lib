package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.missingReport

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.ValidationException
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
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
  @PostMapping("/missingRequest/{reportId}/{variantId}")
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
  ): MissingReportSubmission = missingReportService.createMissingReportSubmission(
    MissingReportSubmissionRequest(
      (authentication as DprAuthAwareAuthenticationToken).getUsername(),
      reportId,
      variantId,
      body,
    ),
  )

  @ConditionalOnBean(MissingReportService::class)
  @GetMapping("/missingRequest/{submissionId}")
  @Operation(
    description = "Get a missing report submission by id",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun getMissingReportSubmissionById(
    @PathVariable submissionId: Int,
  ): MissingReportSubmission = missingReportService.findMissingReportSubmissionBySubmissionId(submissionId)
    ?: throw ValidationException(
      "Missing report submission not found for id=$submissionId",
    )

  @ConditionalOnBean(MissingReportService::class)
  @GetMapping("/missingRequest/{reportId}/{variantId}")
  @Operation(
    description = "Get a missing report submission",
    security = [SecurityRequirement(name = "bearer-jwt")],
  )
  fun getMissingReportSubmission(
    @PathVariable("reportId")
    reportId: String,
    @PathVariable("variantId")
    variantId: String,
    authentication: Authentication,
  ): MissingReportSubmission? = missingReportService.findMissingReportSubmission(
    reportId = reportId,
    variantId = variantId,
    (authentication as DprAuthAwareAuthenticationToken).getUsername(),
  ) ?: throw ValidationException(
    "Missing report submission not found for reportId=$reportId or variantId=$variantId",
  )
}
