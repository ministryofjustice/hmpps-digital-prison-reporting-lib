CREATE SCHEMA product_;
CREATE TABLE product_.product_collection (
  id VARCHAR NOT NULL,
  name VARCHAR NOT NULL,
  version VARCHAR NOT NULL,
  owner_name VARCHAR NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE product_.product_collection_products (
  product_collection_id VARCHAR NOT NULL,
  product_id VARCHAR NOT NULL,
  FOREIGN KEY(product_collection_id) REFERENCES product_.product_collection(id)
);

CREATE TABLE product_.product_collection_attributes (
  product_collection_id VARCHAR NOT NULL,
  attribute_name VARCHAR NOT NULL,
  attribute_value VARCHAR NOT NULL,
  FOREIGN KEY(product_collection_id) REFERENCES product_.product_collection(id)
);