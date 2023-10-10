package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import io.swagger.v3.oas.annotations.media.Schema

data class Count(
  @Schema(example = "501", description = "The total number of records")
  val count: Long,
)
