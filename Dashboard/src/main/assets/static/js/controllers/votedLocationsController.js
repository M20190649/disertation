/**
 * Created by unknown_user on 1/10/2016.
 */
function votedLocationsController($scope) {
    var request = $.ajax({
        url: config.uri + "/locations?topLocations",
        type: "GET",
        connection: "keep-alive",
        contentType: "application/json",
        dataType: "json",
        cache: false,
        processData: false,
    });

    request.done(function(top_locations) {
        $scope.top_locations = top_locations;
        $scope.$apply();
        $scope.redirectToLocation = redirectToLocation; // needs to be in scope
    });
}