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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.vets.model.HandledSpecies;
import org.springframework.samples.petclinic.vets.model.Vet;
import org.springframework.samples.petclinic.vets.model.VetRepository;
import org.springframework.samples.petclinic.vets.model.VetWorkingHour;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Maciej Szarlinski
 */
@WebMvcTest(VetResource.class)
@ActiveProfiles("test")
class VetResourceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    VetRepository vetRepository;

    @Test
    void shouldGetAListOfVets() throws Exception {

        Vet vet = new Vet();
        vet.setId(1);

        given(vetRepository.findAll()).willReturn(List.of(vet));

        mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].handledSpecies").doesNotExist())
            .andExpect(jsonPath("$[0].workingHours").doesNotExist());
    }

    @Test
    void shouldGetVetBookingProfile() throws Exception {
        Vet vet = new Vet();
        vet.setId(1);
        vet.setFirstName("James");
        vet.setLastName("Carter");
        vet.addHandledSpecies(handledSpecies("dog"));
        vet.addHandledSpecies(handledSpecies("cat"));
        vet.addWorkingHour(workingHour(DayOfWeek.MONDAY, "09:00", "16:00"));

        given(vetRepository.findById(1)).willReturn(Optional.of(vet));

        mvc.perform(get("/vets/1/booking-profile").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("James"))
            .andExpect(jsonPath("$.lastName").value("Carter"))
            .andExpect(jsonPath("$.handledSpecies[0]").value("cat"))
            .andExpect(jsonPath("$.handledSpecies[1]").value("dog"))
            .andExpect(jsonPath("$.workingHours[0].dayOfWeek").value("MONDAY"))
            .andExpect(jsonPath("$.workingHours[0].start").value("09:00"))
            .andExpect(jsonPath("$.workingHours[0].end").value("16:00"));
    }

    @Test
    void shouldReturnNotFoundForMissingBookingProfile() throws Exception {
        given(vetRepository.findById(99)).willReturn(Optional.empty());

        mvc.perform(get("/vets/99/booking-profile").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    private HandledSpecies handledSpecies(String name) {
        HandledSpecies species = new HandledSpecies();
        species.setName(name);
        return species;
    }

    private VetWorkingHour workingHour(DayOfWeek dayOfWeek, String start, String end) {
        VetWorkingHour workingHour = new VetWorkingHour();
        workingHour.setDayOfWeek(dayOfWeek);
        workingHour.setStartTime(LocalTime.parse(start));
        workingHour.setEndTime(LocalTime.parse(end));
        return workingHour;
    }
}
