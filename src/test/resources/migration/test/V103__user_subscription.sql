CREATE SCHEMA IF NOT EXISTS subscription_;
CREATE TABLE subscription_.user_subscription (
  id VARCHAR NOT NULL,
  user_id VARCHAR NOT NULL,
  report_id VARCHAR NOT NULL,
  report_variant_id VARCHAR NOT NULL,
  table_id VARCHAR NOT NULL,
  status VARCHAR NOT NULL,
  created_time VARCHAR NOT NULL,
  updated_time VARCHAR,
  PRIMARY KEY(id)
);
CREATE SCHEMA IF NOT EXISTS admin;
CREATE TABLE admin.statement_execution_status (
  id INTEGER NOT NULL,
  status VARCHAR NOT NULL,
  table_id VARCHAR NOT NULL,
  execution_id VARCHAR NOT NULL,
  error_message VARCHAR NOT NULL,
  created_at VARCHAR NOT NULL,
  updated_at VARCHAR NOT NULL,
  PRIMARY KEY(id)
);
