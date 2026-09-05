CREATE TABLE missions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE waypoints (
    id BIGSERIAL PRIMARY KEY,
    mission_id BIGINT NOT NULL REFERENCES missions (id),
    sequence_order INTEGER NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    task_type VARCHAR(255) NOT NULL,
    notes VARCHAR(255)
);

CREATE INDEX idx_waypoints_mission_id ON waypoints (mission_id);

CREATE TABLE mission_phases (
    id BIGSERIAL PRIMARY KEY,
    mission_id BIGINT NOT NULL REFERENCES missions (id),
    name VARCHAR(255) NOT NULL,
    start_offset_minutes INTEGER NOT NULL,
    end_offset_minutes INTEGER NOT NULL,
    notes VARCHAR(255)
);

CREATE INDEX idx_mission_phases_mission_id ON mission_phases (mission_id);

CREATE TABLE resources (
    id BIGSERIAL PRIMARY KEY,
    mission_id BIGINT NOT NULL REFERENCES missions (id),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    call_sign VARCHAR(255),
    available BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_resources_mission_id ON resources (mission_id);
