package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import jakarta.validation.ValidationException
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ConfiguredApiService

@Component
class DatasetHelper {
  fun findDataset(allDatasets: List<Dataset>, id: String): Dataset =
    allDatasets.find { it.id == id.removePrefix(ConfiguredApiService.SCHEMA_REF_PREFIX) }
      ?: throw ValidationException("Invalid dataSetId: $id")
}
