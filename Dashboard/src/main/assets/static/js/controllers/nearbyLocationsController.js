function nearbyLocationsController($scope) {

    nearbyLocationsController.deffered = new jQuery.Deferred();

    $.when(nearbyLocationsController.deffered.promise()).then(function() {
        var queryParams = "topLocations&";
        queryParams += "nearbyLocations&";
        queryParams += "latitude=" + currentLatitude + "&";
        queryParams += "longitude=" + currentLongitude + "&";
        queryParams += "radius=" + 2000;

        var request = $.ajax({
            url: config.uri + "/locations?" + queryParams,
            type: "GET",
            connection: "keep-alive",
            dataType: "json",
            contentType: "application/json",
            cache: false,
        });

        request.done(function(nearby_locations) {
            $scope.nearby_locations = nearby_locations;
            $scope.$apply();
            $scope.redirectToLocation = redirectToLocation; // needs to be in scope
        });
    });
}