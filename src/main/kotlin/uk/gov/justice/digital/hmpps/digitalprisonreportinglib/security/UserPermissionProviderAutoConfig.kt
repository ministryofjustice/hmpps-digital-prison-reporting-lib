package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConditionalOnProperty(name = ["dpr.lib.system.role"])
class UserPermissionProviderAutoConfig {
  @Bean
  @ConditionalOnMissingBean(UserPermissionProvider::class)
  fun userPermissionProvider(@Qualifier("manageUsersWebClient") webClient: WebClient) = UserPermissionProvider(webClient)
}
