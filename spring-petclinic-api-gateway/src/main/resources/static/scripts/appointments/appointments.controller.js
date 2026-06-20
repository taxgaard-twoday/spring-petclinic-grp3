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
        self.selectedOwner = null;
        self.selectedPet = null;
        self.selectedVet = null;
        self.date = null;
        self.time = null;
        self.successMessage = null;
        self.errorMessage = null;
        self.submitting = false;

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
        };

        self.submit = function () {
            clearMessages();

            if (!self.selectedOwner || !self.selectedPet || !self.selectedVet || !self.date || !self.time) {
                self.errorMessage = 'Choose an owner, pet, veterinarian, date, and time';
                return;
            }

            self.submitting = true;

            var data = {
                vetId: self.selectedVet.id,
                start: appointmentStart()
            };

            $http.post(appointmentsUrl(), data, locallyHandledRequest).then(function () {
                self.successMessage = 'Appointment booked';
                self.date = null;
                self.time = null;
                return reloadAppointments();
            }, showError).finally(function () {
                self.submitting = false;
            });
        };

        self.cancel = function (appointment) {
            clearMessages();

            var url = appointmentsUrl() + '/' + appointment.id + '/cancel';
            $http.post(url, null, locallyHandledRequest).then(function () {
                self.successMessage = 'Appointment cancelled';
                return reloadAppointments();
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

        function appointmentsUrl() {
            return 'api/visit/owners/' + self.selectedOwner.id + '/pets/' + self.selectedPet.id + '/appointments';
        }

        function appointmentStart() {
            return $filter('date')(self.date, 'yyyy-MM-dd') + 'T' + $filter('date')(self.time, 'HH:mm');
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
