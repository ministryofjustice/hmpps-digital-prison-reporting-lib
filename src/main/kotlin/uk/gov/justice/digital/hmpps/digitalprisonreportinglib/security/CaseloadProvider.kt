package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.security.oauth2.jwt.Jwt
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload
@Deprecated("Use UserPermissionProvider instead")
interface CaseloadProvider {

  fun getActiveCaseloadId(jwt: Jwt): String

  fun getCaseloads(jwt: Jwt): List<Caseload>
}
