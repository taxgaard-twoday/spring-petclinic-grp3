'use strict';

angular.module('appointments', ['ui.router'])
    .config(['$stateProvider', function ($stateProvider) {
        $stateProvider
            .state('appointments', {
                parent: 'app',
                url: '/appointments',
                template: '<appointments></appointments>'
            })
    }]);
