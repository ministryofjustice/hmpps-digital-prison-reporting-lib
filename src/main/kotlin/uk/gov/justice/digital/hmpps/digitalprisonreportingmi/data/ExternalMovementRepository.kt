package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data

import org.springframework.data.jpa.repository.JpaRepository

interface ExternalMovementRepository : JpaRepository<ExternalMovementEntity, String>, ExternalMovementRepositoryCustom
