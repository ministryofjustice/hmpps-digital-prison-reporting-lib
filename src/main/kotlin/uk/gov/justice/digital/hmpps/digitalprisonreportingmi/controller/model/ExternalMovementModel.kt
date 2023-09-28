package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model

import java.time.LocalDate
import java.time.LocalTime

data class ExternalMovementModel(
  val prisonNumber: String,
  val firstName: String,
  val lastName: String,
  val date: LocalDate,
  val time: LocalTime,
  val from: String?,
  val to: String?,
  val direction: String?,
  val type: String,
  val reason: String,
)
