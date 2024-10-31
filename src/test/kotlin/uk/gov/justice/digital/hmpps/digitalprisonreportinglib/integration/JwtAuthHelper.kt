package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.integration

import io.jsonwebtoken.Jwts
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.Date
import java.util.UUID

@Component
class JwtAuthHelper {
  private val keyPair: KeyPair = createKeyPair()

  companion object {
    fun createKeyPair(): KeyPair {
      val gen = KeyPairGenerator.getInstance("RSA")
      gen.initialize(2048)
      return gen.generateKeyPair()
    }
  }

  @Bean
  fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()

  fun setAuthorisation(
    user: String,
    roles: List<String>,
    scopes: List<String>,
  ): (HttpHeaders) -> Unit {
    val token = createToken(user, scopes, roles)
    return { it.set(HttpHeaders.AUTHORIZATION, "Bearer $token") }
  }

  fun createToken(
    user: String = "prison-reporting-mi-client",
    scopes: List<String> = listOf(),
    roles: List<String> = listOf(),
  ): String {
    val token = createJwt(
      subject = user,
      scope = scopes,
      expiryTime = Duration.ofHours(1L),
      roles = roles,
      )
    return token
  }

  internal fun createJwt(
    subject: String,
    scope: List<String> = listOf(),
    roles: List<String> = listOf(),
    expiryTime: Duration = Duration.ofHours(1),
    jwtId: String = UUID.randomUUID().toString(),
  ): String =
    mapOf(
      "user_name" to subject,
      "client_id" to "prison-reporting-mi-client",
      "authorities" to roles,
      "scope" to scope,
    )
      .let {
        Jwts.builder()
          .id(jwtId)
          .subject(subject)
          .claims(it.toMap())
          .expiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
          .signWith(keyPair.private)
          .compact()
      }
}
