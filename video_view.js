$(function () {

    var deleteVideoRow = function(row) {
        row.remove();
        if($('#video-table tr').length === 1) {
            $('#video-table').addClass('hidden');
            $('#no-videos-message').removeClass('hidden');
        }
    };

    var fillTable = function(response) {
        var userVideos = response.user_videos;
        if(userVideos.length === 0) {
            $("#no-videos-message").removeClass('hidden');
            $("#loading-spinner").addClass('hidden');
            return;
        }

        $("#video-table").append(
            userVideos.map(function(videoInfo) {
                var file_name = videoInfo.file_name;

                var newVideoRow = $(".video-row-template").clone(),
                    videoDate = new Date(videoInfo.video_date);



                newVideoRow.find(".video-name").text(videoDate.toLocaleString());
                newVideoRow.find(".download-video-btn").click(function(event){

                    var dl = document.createElement('a');
                        dl.setAttribute('href', 'http://52.53.190.157:9090/' + file_name + ".mp4");
                        dl.setAttribute('download', '');
                        dl.click();
                        dl.remove();
                });

                newVideoRow.find(".delete-button").click(function(event) {
                    $(this).prop('disabled', true);

                    var row = $(this).parents("tr"),
                        videoID = row.data("videoID");


                    $.ajax({
                        type: "DELETE",
                        url: 'https://stream-cam.herokuapp.com/api/v1/videos/' + videoID,
                        contentType: "application/json; charset=utf-8",
                        dataType: "json",
                        crossDomain: true,
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
        crossDomain: true,
        headers: {
            "token":localStorage.StreamCamToken,
        },
        success: fillTable,
        error: function (jqXHR, textStatus, errorThrown) {
            console.log(jqXHR);
        }
    });

    $("#sign-out-btn").click(function(event) {
        localStorage.removeItem("StreamCamToken");
        localStorage.removeItem("StreamCamUser");
        window.location.href = "index.html";
    });

});