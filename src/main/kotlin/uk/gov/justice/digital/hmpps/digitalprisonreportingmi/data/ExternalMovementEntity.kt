package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "movements_movements", schema = "domain")
data class ExternalMovementEntity(
  @Id val id: Long,
  val prisoner: Long,
  val date: LocalDateTime,
  val time: LocalDateTime,
  val origin: String?,
  val destination: String?,
  val direction: String?,
  val type: String,
  val reason: String,
)
