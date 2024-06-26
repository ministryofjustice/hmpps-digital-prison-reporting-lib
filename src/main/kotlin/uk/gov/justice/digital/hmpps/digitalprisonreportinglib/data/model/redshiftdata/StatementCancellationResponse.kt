package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata

import io.swagger.v3.oas.annotations.media.Schema

data class StatementCancellationResponse(
  @Schema(
    examples = ["true", "false"],
    description = "A value that indicates whether the cancel statement succeeded (true).",
  )
  val cancellationSucceeded: Boolean,
)
