'use strict';

angular.module('appointments')
    .controller('AppointmentsController', ['$http', '$filter', function ($http, $filter) {
        var self = this;
        var locallyHandledRequest = {
            suppressGlobalErrorHandler: true
        };

        self.owners = [];
        self.vets = [];
        self.pets = [];
        self.appointments = [];
        self.availableSlots = [];
        self.selectedOwner = null;
        self.selectedPet = null;
        self.selectedVet = null;
        self.selectedSlot = null;
        self.date = null;
        self.successMessage = null;
        self.errorMessage = null;
        self.slotErrorMessage = null;
        self.loadingSlots = false;
        self.submitting = false;
        var slotRequestId = 0;

        $http.get('api/customer/owners').then(function (resp) {
            self.owners = resp.data;
        });

        $http.get('api/vet/vets').then(function (resp) {
            self.vets = resp.data;
        });

        self.ownerChanged = function () {
            self.selectedPet = null;
            self.appointments = [];
            self.pets = self.selectedOwner && self.selectedOwner.pets ? self.selectedOwner.pets : [];
            resetSlots();
            clearMessages();
        };

        self.petChanged = function () {
            self.appointments = [];
            clearMessages();

            if (!self.selectedOwner || !self.selectedPet) {
                return;
            }

            $http.get(appointmentsUrl(), locallyHandledRequest).then(function (resp) {
                self.appointments = resp.data;
            }, showError);

            loadAvailableSlots();
        };

        self.bookingContextChanged = function () {
            clearMessages();
            loadAvailableSlots();
        };

        self.selectSlot = function (slot) {
            self.selectedSlot = slot;
            clearMessages();
        };

        self.isSelectedSlot = function (slot) {
            return self.selectedSlot && slot && self.selectedSlot.start === slot.start;
        };

        self.submit = function () {
            clearMessages();

            if (!self.selectedOwner || !self.selectedPet || !self.selectedVet || !self.date || !self.selectedSlot) {
                self.errorMessage = 'Choose an owner, pet, veterinarian, date, and available slot';
                return;
            }

            self.submitting = true;

            var data = {
                vetId: self.selectedVet.id,
                start: self.selectedSlot.start
            };

            $http.post(appointmentsUrl(), data, locallyHandledRequest).then(function () {
                self.successMessage = 'Appointment booked';
                self.selectedSlot = null;
                return reloadAppointments().then(loadAvailableSlots);
            }, showError).finally(function () {
                self.submitting = false;
            });
        };

        self.cancel = function (appointment) {
            clearMessages();

            var url = appointmentsUrl() + '/' + appointment.id + '/cancel';
            $http.post(url, null, locallyHandledRequest).then(function () {
                self.successMessage = 'Appointment cancelled';
                return reloadAppointments().then(loadAvailableSlots);
            }, showError);
        };

        self.canCancel = function (appointment) {
            if (!appointment || appointment.status !== 'SCHEDULED') {
                return false;
            }

            var start = new Date(appointment.start);
            return start.getTime() - new Date().getTime() >= 24 * 60 * 60 * 1000;
        };

        self.vetName = function (vetId) {
            var vet = findVet(vetId);
            if (vet) {
                return vet.firstName + ' ' + vet.lastName;
            }

            return 'Vet #' + vetId;
        };

        function reloadAppointments() {
            return $http.get(appointmentsUrl(), locallyHandledRequest).then(function (resp) {
                self.appointments = resp.data;
            });
        }

        function loadAvailableSlots() {
            resetSlots();

            if (!canLoadSlots()) {
                return;
            }

            var currentRequestId = ++slotRequestId;
            self.loadingSlots = true;

            return $http.get(availableSlotsUrl(), locallyHandledRequest).then(function (resp) {
                if (currentRequestId === slotRequestId) {
                    self.availableSlots = resp.data;
                }
            }, function (resp) {
                if (currentRequestId === slotRequestId) {
                    self.slotErrorMessage = errorMessage(resp);
                }
            }).finally(function () {
                if (currentRequestId === slotRequestId) {
                    self.loadingSlots = false;
                }
            });
        }

        function resetSlots() {
            slotRequestId++;
            self.availableSlots = [];
            self.selectedSlot = null;
            self.loadingSlots = false;
            self.slotErrorMessage = null;
        }

        function canLoadSlots() {
            return self.selectedOwner && self.selectedPet && self.selectedVet && self.date;
        }

        function appointmentsUrl() {
            return 'api/visit/owners/' + self.selectedOwner.id + '/pets/' + self.selectedPet.id + '/appointments';
        }

        function availableSlotsUrl() {
            return appointmentsUrl()
                + '/available-slots?vetId='
                + encodeURIComponent(self.selectedVet.id)
                + '&date='
                + encodeURIComponent(appointmentDate());
        }

        function appointmentDate() {
            return $filter('date')(self.date, 'yyyy-MM-dd');
        }

        function findVet(vetId) {
            for (var i = 0; i < self.vets.length; i++) {
                if (self.vets[i].id === vetId) {
                    return self.vets[i];
                }
            }

            return null;
        }

        function showError(resp) {
            self.errorMessage = errorMessage(resp);
        }

        function errorMessage(resp) {
            if (resp && resp.data && resp.data.message) {
                return resp.data.message;
            }
            if (resp && resp.data && resp.data.error) {
                return resp.data.error;
            }

            return 'Appointment request failed';
        }

        function clearMessages() {
            self.successMessage = null;
            self.errorMessage = null;
        }
    }]);
