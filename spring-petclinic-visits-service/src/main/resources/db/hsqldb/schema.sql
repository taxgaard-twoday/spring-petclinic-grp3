DROP TABLE appointments IF EXISTS;
DROP TABLE visits IF EXISTS;

CREATE TABLE visits (
  id          INTEGER IDENTITY PRIMARY KEY,
  pet_id      INTEGER NOT NULL,
  visit_date  DATE,
  description VARCHAR(8192)
);

CREATE INDEX visits_pet_id ON visits (pet_id);

CREATE TABLE appointments (
  id         INTEGER IDENTITY PRIMARY KEY,
  pet_id     INTEGER NOT NULL,
  vet_id     INTEGER NOT NULL,
  start_time TIMESTAMP NOT NULL,
  end_time   TIMESTAMP NOT NULL,
  status     VARCHAR(20) NOT NULL
);

CREATE INDEX appointments_pet_id ON appointments (pet_id);
CREATE INDEX appointments_vet_time ON appointments (vet_id, start_time, end_time);
