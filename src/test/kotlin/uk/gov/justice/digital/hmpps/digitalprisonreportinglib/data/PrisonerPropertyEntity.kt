package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "prisoner_property", schema = "domain")
data class PrisonerPropertyEntity(
  @Id val id: Long,
  val prisoner_id: Long,
  val active: String,
  val seal_mark: String,
  val establishment_id: String,
  val expiry_date: LocalDateTime,
  val living_unit_id: Long,
  val proposed_disposal_date: String,
  val container_code: String,
  val property_only: String,
  val description: String,
)
