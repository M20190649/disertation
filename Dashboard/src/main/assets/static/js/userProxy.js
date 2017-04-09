/**
 * Created by unknown_user on 1/9/2016.
 */

function UserProxy() {

}

UserProxy.prototype.register = function register(user, successHandler, failureHandler) {
    var request = $.ajax({
        url: config.uri + "/users",
        type: "POST",
        connection: "keep-alive",
        contentType: "application/json",
        data: JSON.stringify(user),
        timeout: 10000
    });

    request.done(successHandler);

    request.fail(failureHandler);
}

UserProxy.prototype.login = function login(user, successHandler, failureHandler) {
    var request = $.ajax({
        url: config.uri + "/users/login",
        type: "POST",
        connection: "keep-alive",
        data: JSON.stringify(user),
        contentType: "application/json",
        timeout: 10000
    });

    request.done(successHandler);

    request.fail(failureHandler);
}

UserProxy.prototype.get = function get(userId, successHandler, failureHandler) {
    var request = $.ajax({
        url: config.uri + "/users/" + userId,
        type: "GET",
        conection: "keep-alive",
        contentType: "application/json",
        dataType: "json",
        cache: false,
        processData: false
    });

    request.done(successHandler);

    request.fail(failureHandler);
}

UserProxy.prototype.exists = function get(email, successHandler, failureHandler) {
    var request = $.ajax({
        url: config.uri + "/users?exists=" + email,
        type: "GET",
        conection: "keep-alive",
        contentType: "application/json",
        dataType: "json",
        cache: false,
        processData: false
    });

    request.done(successHandler);

    request.fail(failureHandler);
}

UserProxy.prototype.vote = function vote(userId, locationId, vote) {
    var request = $.ajax({
        url: config.uri + "/users/" +userId + "/locations/" + locationId + "/" + vote,
        type: "POST",
        conection: "keep-alive",
        contentType: "application/json",
        cache: false,
        timeout: 10000,
        processData: false,
    });

    request.done(function (rating) {
        if (rating) {
            $("#Rating").html("" + rating);
        }
    });
}


