'use strict';

angular.module('appointments')
    .controller('AppointmentsController', ['$http', function ($http) {
        var self = this;

        self.owners = [];
        self.vets = [];
        self.pets = [];
        self.appointments = [];
        self.selectedOwner = null;
        self.selectedPet = null;
        self.selectedVet = null;
        self.date = null;
        self.time = null;

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
        };

        self.petChanged = function () {
            self.appointments = [];

            if (!self.selectedOwner || !self.selectedPet) {
                return;
            }

            var url = 'api/visit/owners/' + self.selectedOwner.id + '/pets/' + self.selectedPet.id + '/appointments';
            $http.get(url).then(function (resp) {
                self.appointments = resp.data;
            });
        };

        self.vetName = function (vetId) {
            for (var i = 0; i < self.vets.length; i++) {
                if (self.vets[i].id === vetId) {
                    return self.vets[i].firstName + ' ' + self.vets[i].lastName;
                }
            }

            return 'Vet #' + vetId;
        };
    }]);
