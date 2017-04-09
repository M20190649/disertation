function saveLocationAndRedirect(id, redirect) {

    var request = $.ajax({
        url: config.uri + "/locations/" + id,
        type: "GET",
        connection: "keep-alive",
        contentType: "application/json",
        dataType: "json",
        cache: false,
    });

    request.done(function(place) {
        storage.place = JSON.stringify(place);
        window.location.href = redirect;
    });
}

function saveUserAndRedirect(id, redirect) {

    var request = $.ajax({
        url: config.uri + "/users/" + id,
        type: "GET",
        connection: "keep-alive",
        contentType: "application/json",
        dataType: "json",
        cache: false,
    });

    request.done(function(user) {
        storage.selectedUser = JSON.stringify(user);
        window.location.href = redirect;
    });
}

function redirectToLocation(id, redirect) {

    var request = $.ajax({
        url: config.uri + "/locations/" + id,
        type: "GET",
        connection: "keep-alive",
        headers: {"Authorization": config.accessToken},
        contentType: "application/json",
        dataType: "json",
        cache: false,
    });

    request.done(function(place) {
        storage.place = JSON.stringify(place);
        window.location.href = redirect;
    });
}