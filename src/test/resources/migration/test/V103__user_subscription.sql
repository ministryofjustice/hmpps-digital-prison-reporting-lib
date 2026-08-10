CREATE SCHEMA IF NOT EXISTS subscription_;
CREATE TABLE subscription_.user_subscription (
  id VARCHAR NOT NULL,
  user_id VARCHAR NOT NULL,
  report_id VARCHAR NOT NULL,
  report_variant_id VARCHAR NOT NULL,
  status VARCHAR NOT NULL,
  created_time VARCHAR NOT NULL,
  updated_time VARCHAR,
  PRIMARY KEY(id)
);