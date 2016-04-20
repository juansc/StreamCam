$(function () {

    var fillTable = function(response) {
        var userVideos = response.user_videos;
        if(userVideos.length === 0) {
            $("#no-videos-message").removeClass('hidden');
            $("#loading-spinner").addClass('hidden');
            return;
        }
        //console.log("Processing video");
        //console.log(userVideos);
        $("#video-table").append(
            userVideos.map(function(videoInfo) {
                //console.log("We got a video");

                var newVideoRow = $(".video-row-template").clone(),
                    videoDate = new Date(videoInfo.video_date),
                    duration = videoInfo.video_duration,
                    durationString = "Unavailable";

                if(!(duration === null || duration === undefined)) {
                    durationString = videoInfo.video_duration.minutes + ":" +
                                     videoInfo.video_duration.seconds;
                }

                newVideoRow.find(".video-name").text(videoDate.toGMTString());
                newVideoRow.find(".video-duration").text(durationString);
                newVideoRow.find(".delete-button").click(function(event) {
                    $(this).prop('disabled', true);

                    var row = $(this).parents("tr"),
                        videoID = row.data("videoID");


                    $.ajax({
                        type: "DELETE",
                        url: 'https://stream-cam.herokuapp.com/api/v1/videos/' + videoID,
                        contentType: "application/json; charset=utf-8",
                        dataType: "json",
                        headers: {
                            "token":localStorage.StreamCamToken,
                        },
                        success: function() {
                            deleteVideoRow(row);
                        },
                        error: function (jqXHR, textStatus, errorThrown) {
                            $(this).prop('disabled', false);
                        }
                    });
                });
                newVideoRow.data("videoID", videoInfo.video_id);
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

    var deleteVideoRow = function(row) {
        row.remove();
        if($('#video-table tr').length === 1) {
            $('#video-table').addClass('hidden');
            $('#no-videos-message').removeClass('hidden');
        }
    };

});