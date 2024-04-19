package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "prisoner_status", schema = "domain")
data class PrisonerStatusEntity(
  val number: String,
  @Id val id: Long,
  val legal_status: String,
  val in_out_status: String,
  val booking_status: String,
  val booking_begin: LocalDateTime,
  val active: String,
  val status: String
)
