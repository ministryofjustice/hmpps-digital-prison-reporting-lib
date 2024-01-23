package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "prisoner_alert", schema = "domain")
data class PrisonerAlertEntity(
  @Id val id: Long,
  val date: LocalDateTime,
  val type: String,
  val code: String,
  val description: String,
  val status: String,
  val expiry_date: LocalDateTime,
  val category: String,
  val comment: String,
)
