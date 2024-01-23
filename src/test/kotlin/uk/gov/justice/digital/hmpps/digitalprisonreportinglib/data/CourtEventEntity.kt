package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "court_event", schema = "domain")
data class CourtEventEntity(
  @Id val id: Long,
  val case_id: Long,
  val prisoner_id: Long,
  val date: LocalDateTime,
  val slot: String,
  val start_time: LocalDateTime,
  val end_time: LocalDateTime,
  val subtype_code: String,
  val subtype: String,
  val status: String,
  val destination_code: String,
  val destination: String,
  val direction: String,
)
