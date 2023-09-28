package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.exception

class DuplicateDataSetIdException(val dataSetId: String) : RuntimeException("Error: Duplicate dataSet Id found: $dataSetId")
