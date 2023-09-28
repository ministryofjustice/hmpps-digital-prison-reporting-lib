package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.then
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.ReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.controller.model.VariantDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.MetaData
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.ProductDefinition

class ReportDefinitionServiceTest {

  val minimalDefinition = ProductDefinition(
    id = "1",
    name = "2",
    metaData = MetaData(
      author = "3",
      owner = "4",
      version = "5",
    ),
    report = emptyList(),
  )

  @Test
  fun `Getting report list for user maps correctly`() {
    val expectedResult = ReportDefinition(
      id = "1",
      name = "2",
      variants = listOf(
        VariantDefinition(
          id = "1",
          name = "2",
          resourceName = "3",
        ),
      ),
    )

    val repository = mock<ProductDefinitionRepository> {
      on { getProductDefinitions() } doReturn listOf(minimalDefinition)
    }
    val mapper = mock<ReportDefinitionMapper> {
      on { map(any(), any()) } doReturn expectedResult
    }
    val service = ReportDefinitionService(repository, mapper)

    val actualResult = service.getListForUser(RenderMethod.HTML)

    then(repository).should().getProductDefinitions()
    then(mapper).should().map(minimalDefinition, RenderMethod.HTML)

    assertThat(actualResult).isNotEmpty
    assertThat(actualResult).hasSize(1)
    assertThat(actualResult[0]).isEqualTo(expectedResult)
  }

  @Test
  fun `Getting HTML report list with no matches returns no domains`() {
    val definitionWithNoVariants = ReportDefinition(
      id = "1",
      name = "2",
      variants = emptyList(),
    )
    val repository = mock<ProductDefinitionRepository> {
      on { getProductDefinitions() } doReturn listOf(minimalDefinition)
    }
    val mapper = mock<ReportDefinitionMapper> {
      on { map(any(), any()) } doReturn definitionWithNoVariants
    }
    val service = ReportDefinitionService(repository, mapper)

    val actualResult = service.getListForUser(RenderMethod.HTML)

    assertThat(actualResult).hasSize(0)
  }
}
