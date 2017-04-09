/**
 * Created by unknown_user on 1/10/2016.
 */

function votingPeopleController($scope) {
    var request = $.ajax({
        url: config.uri + "/locations/" + place.id + "/users" + "?voted",
        type: "GET",
        connection: "keep-alive",
        contentType: "application/json",
        dataType: "json",
        cache: false,
        processData: false,
    });

    request.done(function(votingPeople) {
        $scope.votingPeople = votingPeople;
        $scope.$apply();
        $scope.saveUserAndRedirect = saveUserAndRedirect; // needs to be in scope
    });
}