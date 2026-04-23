package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.authentication

import uk.gov.justice.hmpps.kotlin.auth.AuthSource

data class AuthUser(
  val username: String,
  val active: Boolean,
  val name: String,
  val authSource: AuthSource,
  val userId: String,
  val uuid: String,
  val staffId: Int?,
  val activeCaseLoadId: String?,
)
