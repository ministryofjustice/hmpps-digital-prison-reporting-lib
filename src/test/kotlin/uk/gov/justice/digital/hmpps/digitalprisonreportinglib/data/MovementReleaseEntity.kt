package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "movement_release", schema = "domain")
data class MovementReleaseEntity(
  @Id val id: Long,
  val prisoner: Long,
  val date: LocalDateTime,
  val type: String,
  val reason: String,
)
