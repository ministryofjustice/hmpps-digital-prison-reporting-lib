package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Condition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Effect
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType.ROW_LEVEL
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Rule

class JsonFileProductDefinitionRepositoryTest {

  val jsonFileProductDefinitionRepository = JsonFileProductDefinitionRepository(
    listOf("productDefinition.json", "dpd001-court-hospital-movements.json"),
    DefinitionGsonConfig().definitionGson(IsoLocalDateTimeTypeAdaptor()),
  )

  @Test
  fun `returns the correct product definition`() {
    val policy = Policy(
      "caseload",
      ROW_LEVEL,
      listOf("(origin_code='\${caseload}' AND lower(direction)='out') OR (destination_code='\${caseload}' AND lower(direction)='in')"),
      listOf(Rule(Effect.PERMIT, listOf(Condition(exists = listOf("\${caseload}"))))),
    )
    val productDefinition = jsonFileProductDefinitionRepository.getProductDefinition(
      "dpd001-court-hospital-movements",
    )
    Assertions.assertThat(productDefinition).isNotNull
    Assertions.assertThat(productDefinition.id).isEqualTo("dpd001-court-hospital-movements")
    Assertions.assertThat(productDefinition.policy).isEqualTo(listOf(policy))
  }
}
