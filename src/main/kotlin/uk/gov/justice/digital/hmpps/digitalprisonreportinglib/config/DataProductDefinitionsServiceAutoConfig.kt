package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.GsonHttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.util.*

@Configuration
class DataProductDefinitionsServiceAutoConfig() {

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
