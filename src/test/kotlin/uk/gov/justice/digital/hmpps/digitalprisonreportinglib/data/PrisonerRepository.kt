package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.PrisonerEntity

interface PrisonerRepository : CrudRepository<PrisonerEntity, String>
