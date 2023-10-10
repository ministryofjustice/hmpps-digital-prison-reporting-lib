package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.data.repository.CrudRepository

interface PrisonerRepository : CrudRepository<PrisonerEntity, String>
