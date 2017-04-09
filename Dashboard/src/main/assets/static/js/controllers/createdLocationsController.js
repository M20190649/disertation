/**
 * Created by unknown_user on 1/10/2016.
 */
function createdLocationsController($scope) {
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
        var crLoc = [];
        for(var i = 0; i < top_locations.length; i++)
        {
            var loc = top_locations[i];
            var selectedUser = $.parseJSON(storage.selectedUser);
            if(loc.id == selectedUser.id) {
                crLoc.push(loc);
            }
        }

        $scope.createdLocations = crLoc;
        $scope.$apply();
        $scope.redirectToLocation = redirectToLocation; // needs to be in scope
    });
}