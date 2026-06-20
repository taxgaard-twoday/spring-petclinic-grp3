INSERT IGNORE INTO vets VALUES (1, 'James', 'Carter');
INSERT IGNORE INTO vets VALUES (2, 'Helen', 'Leary');
INSERT IGNORE INTO vets VALUES (3, 'Linda', 'Douglas');
INSERT IGNORE INTO vets VALUES (4, 'Rafael', 'Ortega');
INSERT IGNORE INTO vets VALUES (5, 'Henry', 'Stevens');
INSERT IGNORE INTO vets VALUES (6, 'Sharon', 'Jenkins');

INSERT IGNORE INTO specialties VALUES (1, 'radiology');
INSERT IGNORE INTO specialties VALUES (2, 'surgery');
INSERT IGNORE INTO specialties VALUES (3, 'dentistry');

INSERT IGNORE INTO vet_specialties VALUES (2, 1);
INSERT IGNORE INTO vet_specialties VALUES (3, 2);
INSERT IGNORE INTO vet_specialties VALUES (3, 3);
INSERT IGNORE INTO vet_specialties VALUES (4, 2);
INSERT IGNORE INTO vet_specialties VALUES (5, 1);

INSERT IGNORE INTO handled_species VALUES (1, 'cat');
INSERT IGNORE INTO handled_species VALUES (2, 'dog');
INSERT IGNORE INTO handled_species VALUES (3, 'gold fish');
INSERT IGNORE INTO handled_species VALUES (4, 'birds');
INSERT IGNORE INTO handled_species VALUES (5, 'giraf');
INSERT IGNORE INTO handled_species VALUES (6, 'pig');
INSERT IGNORE INTO handled_species VALUES (7, 'small humans');

INSERT IGNORE INTO vet_handled_species VALUES (1, 1);
INSERT IGNORE INTO vet_handled_species VALUES (1, 2);
INSERT IGNORE INTO vet_handled_species VALUES (2, 3);
INSERT IGNORE INTO vet_handled_species VALUES (2, 4);
INSERT IGNORE INTO vet_handled_species VALUES (3, 5);
INSERT IGNORE INTO vet_handled_species VALUES (3, 6);
INSERT IGNORE INTO vet_handled_species VALUES (4, 2);
INSERT IGNORE INTO vet_handled_species VALUES (4, 6);
INSERT IGNORE INTO vet_handled_species VALUES (5, 1);
INSERT IGNORE INTO vet_handled_species VALUES (5, 4);
INSERT IGNORE INTO vet_handled_species VALUES (6, 7);
INSERT IGNORE INTO vet_handled_species VALUES (6, 3);

INSERT IGNORE INTO vet_working_hours VALUES (1, 1, 'MONDAY', '09:00:00', '16:00:00');
INSERT IGNORE INTO vet_working_hours VALUES (2, 1, 'TUESDAY', '09:00:00', '16:00:00');
INSERT IGNORE INTO vet_working_hours VALUES (3, 1, 'WEDNESDAY', '09:00:00', '12:00:00');
INSERT IGNORE INTO vet_working_hours VALUES (4, 2, 'MONDAY', '10:00:00', '15:00:00');
INSERT IGNORE INTO vet_working_hours VALUES (5, 2, 'THURSDAY', '09:00:00', '16:00:00');
INSERT IGNORE INTO vet_working_hours VALUES (6, 3, 'TUESDAY', '09:00:00', '13:00:00');
INSERT IGNORE INTO vet_working_hours VALUES (7, 3, 'FRIDAY', '09:00:00', '16:00:00');
INSERT IGNORE INTO vet_working_hours VALUES (8, 4, 'WEDNESDAY', '11:00:00', '16:00:00');
INSERT IGNORE INTO vet_working_hours VALUES (9, 4, 'THURSDAY', '09:00:00', '14:30:00');
INSERT IGNORE INTO vet_working_hours VALUES (10, 5, 'MONDAY', '09:00:00', '12:15:00');
INSERT IGNORE INTO vet_working_hours VALUES (11, 5, 'FRIDAY', '12:00:00', '16:00:00');
INSERT IGNORE INTO vet_working_hours VALUES (12, 6, 'TUESDAY', '12:00:00', '16:00:00');
INSERT IGNORE INTO vet_working_hours VALUES (13, 6, 'WEDNESDAY', '09:00:00', '15:45:00');
