package org.springframework.samples.petclinic.customers.model;

import java.util.Comparator;

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
class PetRepositoryTest {

    @Autowired
    PetRepository petRepository;

    @Test
    void shouldLoadBookingSpeciesPetTypesAndSamplePets() {
        assertThat(petRepository.findPetTypes().stream()
            .sorted(Comparator.comparing(PetType::getId)))
            .extracting(PetType::getName)
            .containsExactly("cat", "dog", "gold fish", "birds", "giraf", "pig", "small humans");

        assertThat(petRepository.findAll())
            .extracting(pet -> pet.getType().getName())
            .contains("cat", "dog", "gold fish", "birds", "giraf", "pig", "small humans");
    }
}
