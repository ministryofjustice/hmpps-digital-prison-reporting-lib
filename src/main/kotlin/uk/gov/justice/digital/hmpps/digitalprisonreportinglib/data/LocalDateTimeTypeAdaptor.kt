package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import java.time.LocalDateTime

interface LocalDateTimeTypeAdaptor : JsonSerializer<LocalDateTime?>, JsonDeserializer<LocalDateTime?>
