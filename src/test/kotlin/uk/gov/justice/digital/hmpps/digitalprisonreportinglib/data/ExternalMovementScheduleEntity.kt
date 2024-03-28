package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "movement_schedule", schema = "domain")
data class ExternalMovementScheduleEntity(
  @Id val id: Long,
  val prisoner: Long,
  val prisoner_id: Long,
  val date: LocalDateTime,
  val time: LocalDateTime,
  val slot: String,
  val start_time: LocalDateTime,
  val end_time: LocalDateTime,
  val return_time: LocalDateTime,
  val origin: String?,
  val origin_code: String?,
  val destination: String?,
  val destination_code: String?,
  val direction: String?,
  val escorted: String?,
  val `class`: String?,
  val type: String,
  val type_code: String,
  val subtype: String,
  val status: String,
  val transfer_approved: String,
  val wait_list_status: String,
)
