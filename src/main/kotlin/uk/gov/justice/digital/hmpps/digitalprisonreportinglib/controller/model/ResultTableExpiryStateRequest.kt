package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import jakarta.validation.constraints.Size

data class ResultTableExpiryStateRequest(
  @field:Size(max = 200)
  val tableIds: List<String>,
)
