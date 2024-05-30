package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TableIdGenerator {

  fun generateNewExternalTableId(): String {
    return "_" + UUID.randomUUID().toString().replace("-", "_")
  }
}
