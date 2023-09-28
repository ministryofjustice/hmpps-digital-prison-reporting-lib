package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data

import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ExternalMovementFilter

interface ExternalMovementRepositoryCustom {

  fun list(selectedPage: Long, pageSize: Long, sortColumn: String, sortedAsc: Boolean, filters: Map<ExternalMovementFilter, Any>): List<ExternalMovementPrisonerEntity>
  fun count(filters: Map<ExternalMovementFilter, Any>): Long
}
