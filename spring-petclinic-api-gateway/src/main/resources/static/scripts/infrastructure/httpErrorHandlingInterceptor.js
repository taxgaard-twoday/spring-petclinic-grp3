'use strict';

/**
 * Global HTTP errors handler.
 */
angular.module('infrastructure')
    .factory('HttpErrorHandlingInterceptor', ['$q', function ($q) {
        return {
            responseError: function (response) {
                if (response.config && response.config.suppressGlobalErrorHandler) {
                    return $q.reject(response);
                }

                var error = response.data;
                var message = error && error.error ? error.error : "Request failed";

                if (error && error.errors) {
                    message += "\r\n" + error.errors.map(function (e) {
                        return e.field + ": " + e.defaultMessage;
                    }).join("\r\n");
                } else if (error && error.message) {
                    message += "\r\n" + error.message;
                }

                alert(message);
                return response;
            }
        }
    }]);
