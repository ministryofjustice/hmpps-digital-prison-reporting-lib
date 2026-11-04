package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import com.google.gson.Gson
import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionDecoder

@Configuration
class DefinitionKotlinxConfig {

  @Bean("dprDefinitionKotlinx")
  fun kotlinxJson(): Json = Json {
    prettyPrint = false
    allowComments = false
    allowTrailingComma = false
    allowStructuredMapKeys = false
    allowSpecialFloatingPointValues = false
    coerceInputValues = false
    encodeDefaults = true
    explicitNulls = true
    ignoreUnknownKeys = false
    isLenient = false
  }

  @Bean
  fun productDefinitionDecoder(
    dprDefinitionGson: Gson,
    dprDefinitionKotlin: Json,
  ): ProductDefinitionDecoder = ProductDefinitionDecoder(dprDefinitionGson, dprDefinitionKotlin)
}
