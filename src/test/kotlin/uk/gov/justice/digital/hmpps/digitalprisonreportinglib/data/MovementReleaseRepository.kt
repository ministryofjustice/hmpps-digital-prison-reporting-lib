package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.data.jpa.repository.JpaRepository

interface MovementReleaseRepository : JpaRepository<MovementReleaseEntity, String>
