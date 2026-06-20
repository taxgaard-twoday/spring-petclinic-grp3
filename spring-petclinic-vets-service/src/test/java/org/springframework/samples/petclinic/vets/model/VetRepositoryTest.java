package org.springframework.samples.petclinic.vets.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.config.import=",
    "spring.cloud.config.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false"
})
@ActiveProfiles("test")
class VetRepositoryTest {

    @Autowired
    VetRepository vetRepository;

    @Test
    void shouldLoadTreatmentSpecialtiesHandledSpeciesAndWorkingHours() {
        Vet vet = vetRepository.findById(2).orElseThrow();

        assertThat(vet.getSpecialties())
            .extracting(Specialty::getName)
            .containsExactly("radiology");

        assertThat(vet.getHandledSpecies())
            .extracting(HandledSpecies::getName)
            .containsExactly("birds", "gold fish");

        assertThat(vet.getWorkingHours())
            .extracting(workingHour -> workingHour.getDayOfWeek().name() + " " + workingHour.getStartTime() + "-" + workingHour.getEndTime())
            .containsExactly("MONDAY 10:00-15:00", "THURSDAY 09:00-16:00");
    }
}
