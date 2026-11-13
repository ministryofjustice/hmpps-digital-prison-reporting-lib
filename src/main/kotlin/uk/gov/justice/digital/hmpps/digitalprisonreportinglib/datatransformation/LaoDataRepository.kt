package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.datatransformation

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper
import java.time.ZoneId
import java.time.ZonedDateTime

@Repository
class LaoDataRepository : RepositoryHelper() {
  private val utcZone = ZoneId.of("Z")

  fun getLaoExclusionsForUser(userId: String) = populateNamedParameterJdbcTemplate().query(
    """
      SELECT
          crn,
          'EXCLUSION' AS type,
          user_id,
          reason,
          since,
          until
      FROM product_.lao_exclusions
      WHERE user_id = :user_id AND since <= NOW() AND until >= NOW();
    """.trimIndent(),
    mapOf("user_id" to userId),
  ) { rs, _ ->
    val until = rs.getTimestamp("until")
    LaoData(
      rs.getString("crn"),
      rs.getString("type"),
      rs.getString("user_id"),
      rs.getString("reason"),
      ZonedDateTime.ofInstant(rs.getTimestamp("since").toInstant(), utcZone),
      if (until != null) ZonedDateTime.ofInstant(until.toInstant(), utcZone) else null,
    )
  }

  fun getLaoRestrictionsForCrns(crns: List<String>): List<LaoData> = populateNamedParameterJdbcTemplate().query(
    """
      SELECT
          crn,
          'RESTRICTION' AS type,
          user_id,
          reason,
          since,
          until
      FROM product_.lao_restrictions
      WHERE crn in (:crns) AND since <= NOW() AND until >= NOW();
    """.trimIndent(),
    mapOf("crns" to crns),
  ) { rs, _ ->
    val until = rs.getTimestamp("until")
    LaoData(
      rs.getString("crn"),
      rs.getString("type"),
      rs.getString("user_id"),
      rs.getString("reason"),
      ZonedDateTime.ofInstant(rs.getTimestamp("since").toInstant(), utcZone),
      if (until != null) ZonedDateTime.ofInstant(until.toInstant(), utcZone) else null,
    )
  }
}

data class LaoData(
  val crn: String,
  val type: String,
  val userId: String,
  val reason: String? = "No reason was given for this $type",
  val since: ZonedDateTime,
  val until: ZonedDateTime?,
)
