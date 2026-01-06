package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ExternalMovementEntity

interface ExternalMovementRepository : JpaRepository<ExternalMovementEntity, String>
