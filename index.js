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
	        success: function (response) {
	            if(response.status === 200) {
	            	clearErrorMessage();
	            	window.location.href = "video_view.html";
	            }
	        },
	        error: function (jqXHR, textStatus, errorThrown) {
	        	var status = jqXHR.status,
	        	    message = "";

	        	switch(status) {
	        		case 400:
	        			message = "Please provide username and password";
	        			break;
	        		case 401:
	        			message = "Incorrect password";
	        			break;
	        		case 404:
	        			message = "Username not found.";
	        			break;
	        		default:
	        			message = "Unknown error";
	        			break;
	        	}
	        	handleError(message);
	        }
    	});
    };

    var handleError = function (errorMessage) {
    	$("#error-message").html(errorMessage);
    	$("#user-name").val('');
    	$("#password").val('');
    }

    var clearErrorMessage = function() {
    	$("#error-message").html("");
    }

    $("#login-module").submit(function() {
    	loginRequest();
    	return false;
    });

});