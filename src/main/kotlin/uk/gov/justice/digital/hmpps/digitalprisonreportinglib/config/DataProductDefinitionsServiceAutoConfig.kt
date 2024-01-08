package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.converter.json.GsonHttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.util.*


@Configuration
@ConditionalOnProperty(name = ["dpr.lib.dataProductDefinitions.host"])
class DataProductDefinitionsServiceAutoConfig(
) {

  @Bean
  @Qualifier("definitionsWebClient")
  fun definitionsWebClient(dprDefinitionGson: Gson): RestTemplate {
    val restTemplate = RestTemplate(
      listOf(GsonHttpMessageConverter(dprDefinitionGson)),
    )
//    val strategies = ExchangeStrategies
//      .builder()
//      .codecs { clientDefaultCodecsConfigurer: ClientCodecConfigurer ->
//        clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(
//          Jackson2JsonEncoder(
//            ObjectMapper(),
//            MediaType.APPLICATION_JSON,
//          ),
//        )
//        clientDefaultCodecsConfigurer.defaultCodecs().configureDefaultCodec{ _ -> dprDefinitionGson}
//      }.build()
//    return WebClient.builder().baseUrl(definitionsHost).exchangeStrategies(strategies).build()
    return restTemplate
  }
}
