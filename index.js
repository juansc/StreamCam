$(function () {

    var loginRequest = function () {
    	var requestObject = JSON.stringify({
    			user: $('#user-name').val(),
    			password: $('#password').val(),
    		});
    	$.ajax({
    		type: "POST",
	        url: "https://stream-cam.herokuapp.com/api/v1/authenticate",
	        data: requestObject,
	        contentType: "application/json; charset=utf-8",
	        dataType: "json",
	        success: function (data) {
	            console.log(data.toString());
	        },
	        error: function (jqXHR, textStatus, errorThrown) {
	            console.log(errorThrown);
	        }
    	});


    };

    $("#login-module").submit(function() {
    	loginRequest();
    	return false;
    });

});