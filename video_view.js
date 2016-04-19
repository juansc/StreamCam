$(function () {

    var fillTable = function(response) {
        var userVideos = response.user_videos;
        if(userVideos.length === 0) {
            $("#no-videos-message").removeClass('hidden');
            $("#loading-spinner").addClass('hidden');
            return;
        }
        console.log("Processing video");
        console.log(userVideos);
        $("#video-table").append(
            userVideos.map(function(videoInfo) {
                console.log("We got a video");
                console.log(videoInfo);

                var newVideoRow = $(".video-row-template").clone(),
                    videoDate = new Date(videoInfo.video_date),
                    durationString = videoInfo.video_duration.minutes + ":" +
                                     videoInfo.video_duration.seconds;

                newVideoRow.find(".video-name").text(videoDate.toGMTString());
                newVideoRow.find(".video-duration").text(durationString);
                newVideoRow.removeClass('video-row-template');
                newVideoRow.removeClass('hidden');
                return newVideoRow;
            })
        );
        $("#video-table").removeClass('hidden');
        $("#loading-spinner").addClass('hidden');
    };

    $.ajax({
        type: "GET",
        url: 'https://stream-cam.herokuapp.com/api/v1/videos/' + localStorage.StreamCamUser,
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        headers: {
            "token":localStorage.StreamCamToken,
        },
        success: fillTable,
        error: function (jqXHR, textStatus, errorThrown) {
            console.log(jqXHR);
        }
    });



});