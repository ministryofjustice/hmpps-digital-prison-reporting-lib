package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.datatransformation

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.context.ExecutionContext
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.PolicyBasedDataChange
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.Policy

class LaoDataTransformer: DataTransformer {
  override fun transform(
    data: List<Map<String, Any?>>,
    policy: Policy,
    executionContext: ExecutionContext,
  ): Pair<List<Map<String, Any?>>, List<PolicyBasedDataChange>> {

//
//    ${_action!!.first()} NOT IN (
//      WITH exclusions_cte as (
//        SELECT crn FROM product_.lao_exclusions e WHERE e.user_id = '${executionContext.userInfo.username}' AND e.since <= NOW() AND e.until >= NOW()
//      ),
//      restrictions_cte as (
//        SELECT crn FROM product_.lao_restrictions r
//        WHERE r.since <= NOW() AND r.until >= NOW()
//        GROUP BY crn
//        HAVING NOT BOOL_OR(user_id = '${executionContext.userInfo.username}')
//    )
//    SELECT crn FROM exclusions_cte UNION SELECT crn FROM restrictions_cte
//    )
//    """.trimIndent()

    val laoCrnColumn = policy.action.first()
    if (laoCrnColumn.isEmpty()) {
      throw ValidationException("`action` was empty for LAO policy type, should contain one entry which is the name of the column representing CRN.")
    }

    val filteredData = data.filterNot {
      it["has_related_lao_exclusion"] as Boolean || it["has_related_lao_restriction"] as Boolean
    }

    return Pair(filteredData, emptyList())
  }
}