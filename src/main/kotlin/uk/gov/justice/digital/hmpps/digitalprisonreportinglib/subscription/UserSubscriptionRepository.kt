package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.RowMapperResultSetExtractor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.RepositoryHelper
import java.sql.ResultSet

@Repository
@ConditionalOnBean(UserSubscriptionService::class)
class UserSubscriptionRepository : RepositoryHelper() {

  companion object {
    const val FIND_BY_ID = """
      SELECT id, user_id, report_id, report_variant_id, status, created_time, updated_time
      FROM subscription_.user_subscription
      WHERE id = :id
    """
    const val FIND_BY_USER_ID = """
      SELECT id, user_id, report_id, report_variant_id, status, created_time, updated_time
      FROM subscription_.user_subscription
      WHERE user_id = :user_id
    """
    const val FIND_BY_USER_ID_AND_REPORT = """
      SELECT id, user_id, report_id, report_variant_id, status, created_time, updated_time
      FROM subscription_.user_subscription
      WHERE user_id = :user_id AND report_id = :report_id AND report_variant_id = :report_variant_id
    """
    const val INSERT_USER_SUBSCRIPTION = """
      INSERT INTO subscription_.user_subscription
      (id, user_id, report_id, report_variant_id, status, created_time)
      VALUES ;;insertAttrValues;;;
    """
    const val MERGE_USER_SUBSCRIPTION = """
      MERGE INTO subscription_.user_subscription
      USING (SELECT :id as id, :status as status, :updated_time as updated_time) as source
      on subscription_.user_subscription.id = source.id 
      WHEN MATCHED THEN
      UPDATE SET id = source.id, status = source.status, updated_time = source.updated_time
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
      "status" to userSubscription.status,
      "created_time" to userSubscription.createdTime,
    )

    template.update(
      """
      BEGIN READ WRITE;
        $INSERT_USER_SUBSCRIPTION
      COMMIT TRANSACTION
      """.trimIndent(),
      namedParameters,
    )
    return findById(userSubscription.id)
  }

  fun updateSubscription(userSubscription: UserSubscription): UserSubscription? {
    val template = populateNamedParameterJdbcTemplate()

    val namedParameters = mutableMapOf<String, Any>(
      "id" to userSubscription.id,
      "status" to userSubscription.status,
      "updated_time" to userSubscription.updatedTime!!,
    )

    template.update(
      """
      BEGIN READ WRITE;
        $MERGE_USER_SUBSCRIPTION 
      COMMIT TRANSACTION
      """.trimIndent(),
      namedParameters,
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
    val createdAt = rs.getTimestamp("created_time")
    val updatedAt = rs.getTimestamp("updated_time")
    return UserSubscription(id, userId, reportId, reportVariantId, status, createdAt.toLocalDateTime(), updatedAt.toLocalDateTime())
  }
}
