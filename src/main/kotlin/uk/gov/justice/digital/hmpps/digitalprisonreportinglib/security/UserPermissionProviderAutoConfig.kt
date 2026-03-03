package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import java.time.Duration

@Configuration
@ConditionalOnProperty(name = ["dpr.lib.system.role"])
class UserPermissionProviderAutoConfig(
  @Value("\${hmpps.manage-users.url}")
  private val manageUsersApiUri: String,
  @Value("\${api.timeout:20s}")
  private val healthTimeout: Duration,
) {
  @Bean
  @ConditionalOnMissingBean(UserPermissionProvider::class)
  fun userPermissionProvider(@Qualifier("manageUsersWebClient") webClient: WebClient): UserPermissionProvider = DefaultUserPermissionProvider(webClient)

  @Bean
  @ConditionalOnMissingBean(UserPermissionProvider::class)
  fun manageUsersWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    registrationId = "DPR_LIB_API",
    url = manageUsersApiUri,
    healthTimeout,
  )
}
