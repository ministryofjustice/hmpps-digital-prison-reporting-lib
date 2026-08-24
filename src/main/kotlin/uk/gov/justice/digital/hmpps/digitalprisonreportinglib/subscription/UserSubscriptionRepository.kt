package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.RowMapperResultSetExtractor
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types

@Repository
class UserSubscriptionRepository : RepositoryHelper() {

  companion object {
    const val FIND_BY_ID = """
      SELECT id, user_id, report_id, report_variant_id, table_id, status, created_time, updated_time
      FROM subscription_.user_subscription
      WHERE id = :id
    """
    const val FIND_BY_USER_ID = """
      SELECT id, user_id, report_id, report_variant_id, table_id, status, created_time, updated_time
      FROM subscription_.user_subscription
      WHERE user_id = :user_id
    """
    const val FIND_REPORTS_BY_USER_ID = """
      WITH latest_report AS (
          SELECT 
              table_id,
              status,
              created_at,
              ROW_NUMBER() OVER (
                  PARTITION BY table_id 
                  ORDER BY created_at DESC
              ) as rn
          FROM admin.statement_execution_status
      )
      SELECT us.id, us.user_id, us.report_id, us.report_variant_id, us.table_id, us.status, us.created_time, us.updated_time, ses.status as report_status, ses.created_at as report_updated_time
      FROM subscription_.user_subscription us
      LEFT JOIN latest_report ses ON us.table_id = ses.table_id and ses.rn = 1
      WHERE us.user_id = :user_id
    """
    const val FIND_BY_USER_ID_AND_REPORT = """
      SELECT id, user_id, report_id, report_variant_id, table_id, status, created_time, updated_time
      FROM subscription_.user_subscription
      WHERE user_id = :user_id AND report_id = :report_id AND report_variant_id = :report_variant_id
    """
    const val INSERT_USER_SUBSCRIPTION = """
      INSERT INTO subscription_.user_subscription
      (id, user_id, report_id, report_variant_id, table_id, status, created_time)
      VALUES (:id, :user_id, :report_id, :report_variant_id, :table_id, :status, :created_time)
    """
    const val MERGE_USER_SUBSCRIPTION = """
      MERGE INTO subscription_.user_subscription
      USING (SELECT :id as id, :status as status, :updated_time as updated_time) as source
      on subscription_.user_subscription.id = source.id 
      WHEN MATCHED THEN
      UPDATE SET id = source.id, status = source.status, updated_time = source.updated_time::timestamp
      WHEN NOT MATCHED 
      THEN INSERT VALUES (source.id, source.status, source.updated_time);
    """
  }

  open fun findById(id: String): UserSubscription? = populateNamedParameterJdbcTemplate()
    .query(
      FIND_BY_ID,
      mapOf("id" to id),
      RowMapperResultSetExtractor(UserSubscriptionRowMapper()),
    ).firstOrNull()

  open fun findByUserId(userId: String): List<UserSubscription> = populateNamedParameterJdbcTemplate()
    .query(
      FIND_BY_USER_ID,
      mapOf("user_id" to userId),
      RowMapperResultSetExtractor(UserSubscriptionRowMapper()),
    )

  fun findReportsByUserId(userId: String): List<UserReportSubscription> = populateNamedParameterJdbcTemplate()
    .query(
      FIND_REPORTS_BY_USER_ID,
      mapOf("user_id" to userId),
      RowMapperResultSetExtractor(UserReportSubscriptionRowMapper()),
    )

  open fun findByUserIdAndReport(userId: String, reportId: String, reportVariantId: String): UserSubscription? {
    val results = populateNamedParameterJdbcTemplate().query(
      FIND_BY_USER_ID_AND_REPORT,
      mapOf(
        "user_id" to userId,
        "report_id" to reportId,
        "report_variant_id" to reportVariantId,
      ),
      RowMapperResultSetExtractor(UserSubscriptionRowMapper()),
    )
    return results.firstOrNull()
  }

  fun create(userSubscription: UserSubscription): UserSubscription? {
    val template = populateNamedParameterJdbcTemplate()

    val namedParameters = mutableMapOf<String, Any>(
      "id" to userSubscription.id,
      "user_id" to userSubscription.userId,
      "report_id" to userSubscription.reportId,
      "report_variant_id" to userSubscription.reportVariantId,
      "table_id" to userSubscription.tableId,
      "status" to userSubscription.status,
      "created_time" to Timestamp.valueOf(userSubscription.createdTime),
    )

    val parameters = MapSqlParameterSource(namedParameters)
    parameters.addValue("created_time", Timestamp.valueOf(userSubscription.createdTime), Types.TIMESTAMP)

    template.update(
      """
        $INSERT_USER_SUBSCRIPTION
      """.trimIndent(),
      parameters,
    )
    return findById(userSubscription.id)
  }

  fun updateSubscription(userSubscription: UserSubscription): UserSubscription? {
    val template = populateNamedParameterJdbcTemplate()

    val namedParameters = mutableMapOf<String, Any>(
      "id" to userSubscription.id,
      "status" to userSubscription.status,
      "updated_time" to Timestamp.valueOf(userSubscription.updatedTime!!),
    )

    val parameters = MapSqlParameterSource(namedParameters)
    parameters.addValue("updated_time", Timestamp.valueOf(userSubscription.updatedTime), Types.TIMESTAMP)

    template.update(
      """
      BEGIN READ WRITE;
        $MERGE_USER_SUBSCRIPTION 
      COMMIT TRANSACTION
      """.trimIndent(),
      parameters,
    )
    return findById(userSubscription.id)
  }
}

class UserSubscriptionRowMapper : RowMapper<UserSubscription> {
  override fun mapRow(
    rs: ResultSet,
    rowNum: Int,
  ): UserSubscription {
    val id = rs.getString("id")
    val userId = rs.getString("user_id")
    val reportId = rs.getString("report_id")
    val reportVariantId = rs.getString("report_variant_id")
    val status = rs.getString("status")
    val tableId = rs.getString("table_id")
    val createdAt = rs.getTimestamp("created_time")
    val updatedAt = rs.getTimestamp("updated_time")
    return UserSubscription(id, userId, reportId, reportVariantId, status, tableId, createdAt.toLocalDateTime(), updatedAt?.toLocalDateTime())
  }
}

class UserReportSubscriptionRowMapper : RowMapper<UserReportSubscription> {
  override fun mapRow(
    rs: ResultSet,
    rowNum: Int,
  ): UserReportSubscription {
    val userId = rs.getString("user_id")
    val reportId = rs.getString("report_id")
    val reportVariantId = rs.getString("report_variant_id")
    val tableId = rs.getString("table_id")
    val reportStatus = rs.getString("report_status") ?: "PENDING"
    val reportUpdatedTime = rs.getTimestamp("report_updated_time")
    return UserReportSubscription(userId, reportId, reportVariantId, tableId, reportStatus, reportUpdatedTime?.toLocalDateTime())
  }
}
