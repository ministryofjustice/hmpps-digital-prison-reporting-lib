package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TableIdGeneratorTest {
  @Test
  fun `generateExternalTableId generates a table UUID starting with an underscore and containing underscores instead of hyphens`() {
    val tableIdGenerator = TableIdGenerator()
    val tableId = tableIdGenerator.generateNewExternalTableId()
    assertThat(tableId).startsWith("_")
    assertThat(tableId).doesNotContain("-")
    assertThat(tableId).hasSize(37)
  }
}
