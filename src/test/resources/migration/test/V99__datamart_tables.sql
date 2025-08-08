CREATE TABLE movement_movement (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    prisoner BIGINT NOT NULL,
    date TIMESTAMP NOT NULL,
    time TIMESTAMP NOT NULL,
    direction TEXT,
    type TEXT,
    origin_code TEXT,
    origin TEXT,
    destination TEXT,
    destination_code TEXT,
    reason TEXT NOT NULL,
    IS_CLOSED boolean DEFAULT NULL
);

CREATE TABLE prisoner_prisoner (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    number text NOT NULL,
    firstname text NOT NULL,
    lastname text NOT NULL,
    living_unit_reference BIGINT
);
