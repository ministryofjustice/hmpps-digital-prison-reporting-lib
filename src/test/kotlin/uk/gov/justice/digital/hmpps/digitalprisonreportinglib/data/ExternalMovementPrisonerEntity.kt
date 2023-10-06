package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import java.time.LocalDateTime

data class ExternalMovementPrisonerEntity(
  val number: String,
  val firstName: String,
  val lastName: String,
  val date: LocalDateTime,
  val time: LocalDateTime,
  val origin: String?,
  val destination: String?,
  val direction: String?,
  val type: String,
  val reason: String,
)
