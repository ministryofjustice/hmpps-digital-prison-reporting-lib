package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data

import org.springframework.data.repository.CrudRepository

interface PrisonerRepository : CrudRepository<PrisonerEntity, String>
