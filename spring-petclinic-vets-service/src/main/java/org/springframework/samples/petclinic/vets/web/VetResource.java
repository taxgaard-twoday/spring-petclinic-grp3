/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.vets.web;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.samples.petclinic.vets.model.Vet;
import org.springframework.samples.petclinic.vets.model.VetWorkingHour;
import org.springframework.samples.petclinic.vets.model.VetRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Maciej Szarlinski
 */
@RequestMapping("/vets")
@RestController
class VetResource {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final VetRepository vetRepository;

    VetResource(VetRepository vetRepository) {
        this.vetRepository = vetRepository;
    }

    @GetMapping
    @Cacheable("vets")
    public List<Vet> showResourcesVetList() {
        return vetRepository.findAll();
    }

    @GetMapping("/{vetId}/booking-profile")
    public BookingProfile showBookingProfile(@PathVariable("vetId") int vetId) {
        Vet vet = vetRepository.findById(vetId)
            .orElseThrow(() -> new ResourceNotFoundException("Vet " + vetId + " not found"));

        return BookingProfile.from(vet);
    }

    record BookingProfile(
        Integer id,
        String firstName,
        String lastName,
        List<String> handledSpecies,
        List<WorkingHour> workingHours
    ) {

        static BookingProfile from(Vet vet) {
            return new BookingProfile(
                vet.getId(),
                vet.getFirstName(),
                vet.getLastName(),
                vet.getHandledSpecies().stream()
                    .map(species -> species.getName())
                    .toList(),
                vet.getWorkingHours().stream()
                    .map(WorkingHour::from)
                    .toList()
            );
        }
    }

    record WorkingHour(
        String dayOfWeek,
        String start,
        String end
    ) {

        static WorkingHour from(VetWorkingHour workingHour) {
            return new WorkingHour(
                workingHour.getDayOfWeek().name(),
                workingHour.getStartTime().format(TIME_FORMATTER),
                workingHour.getEndTime().format(TIME_FORMATTER)
            );
        }
    }
}
