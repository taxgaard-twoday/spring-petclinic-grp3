DROP TABLE vet_specialties IF EXISTS;
DROP TABLE vet_handled_species IF EXISTS;
DROP TABLE vet_working_hours IF EXISTS;
DROP TABLE handled_species IF EXISTS;
DROP TABLE vets IF EXISTS;
DROP TABLE specialties IF EXISTS;

CREATE TABLE vets (
  id         INTEGER IDENTITY PRIMARY KEY,
  first_name VARCHAR(30),
  last_name  VARCHAR(30)
);
CREATE INDEX vets_last_name ON vets (last_name);

CREATE TABLE specialties (
  id   INTEGER IDENTITY PRIMARY KEY,
  name VARCHAR(80)
);
CREATE INDEX specialties_name ON specialties (name);

CREATE TABLE vet_specialties (
  vet_id       INTEGER NOT NULL,
  specialty_id INTEGER NOT NULL
);
ALTER TABLE vet_specialties ADD CONSTRAINT fk_vet_specialties_vets FOREIGN KEY (vet_id) REFERENCES vets (id);
ALTER TABLE vet_specialties ADD CONSTRAINT fk_vet_specialties_specialties FOREIGN KEY (specialty_id) REFERENCES specialties (id);

CREATE TABLE handled_species (
  id   INTEGER IDENTITY PRIMARY KEY,
  name VARCHAR(80)
);
CREATE INDEX handled_species_name ON handled_species (name);

CREATE TABLE vet_handled_species (
  vet_id     INTEGER NOT NULL,
  species_id INTEGER NOT NULL
);
ALTER TABLE vet_handled_species ADD CONSTRAINT fk_vet_handled_species_vets FOREIGN KEY (vet_id) REFERENCES vets (id);
ALTER TABLE vet_handled_species ADD CONSTRAINT fk_vet_handled_species_species FOREIGN KEY (species_id) REFERENCES handled_species (id);

CREATE TABLE vet_working_hours (
  id          INTEGER IDENTITY PRIMARY KEY,
  vet_id      INTEGER NOT NULL,
  day_of_week VARCHAR(9) NOT NULL,
  start_time  TIME NOT NULL,
  end_time    TIME NOT NULL
);
ALTER TABLE vet_working_hours ADD CONSTRAINT fk_vet_working_hours_vets FOREIGN KEY (vet_id) REFERENCES vets (id);
