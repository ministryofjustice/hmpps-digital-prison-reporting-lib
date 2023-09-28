package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "prisoner_prisoner", schema = "domain")
class PrisonerEntity(
  @Id
  val id: Long,
  val number: String,
  @Column(name = "firstname")
  val firstName: String,
  @Column(name = "lastname")
  val lastName: String,
  @Column(name = "living_unit_reference")
  val livingUnitReference: Long?,
)
