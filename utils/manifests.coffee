db_client = require '../database/database_client'
token = require '../utils/token'

exports.appendToVideoManifest = (req, res) ->
  user_token = req.body.token or req.headers['x-access-token'] if req
  unless user_token
    return res.status(403).json
      status: 403
      message: "Forbidden action"

  decoded = token.decodeToken user_token
  unless decoded
    return res.status(400).json
      status: 400
      message: "Invalid token"

  unless req.body.JSONlocation
    return res.status(400).json
      status: 400
      message: "No location included"

  try
    location = JSON.parse req.body.JSONlocation
  catch e
    return res.status(400).json
      status: 400
      message: "Could not parse location."

  video_id = req.params.video_id
  # First we check that the video
  # exists and belongs to the requester
  db_client.query
    text: "SELECT EXISTS(
           SELECT 1 FROM
           users INNER JOIN user_videos USING (user_id)
           WHERE video_id=$1 AND username=$2)"
    values: [video_id, decoded.user]
  , (err, result) ->
    if err
      return res.status(500).json
        status: 500
        message: "Server Error"
    if !result.rows[0].exists
      return res.status(403).json
        status: 403
        message: "Authorization failed."
    else
      db_client.query
        text: "INSERT INTO video_manifests
               (video_id, location_timestamp, address, latitude, longitude)
               VALUES
               ($1,$2,$3,$4,$5)"
        values: [
          video_id,
          location.timestamp,
          location.adress,
          location.latitude,
          location.longitude
        ]
      , (err, result) ->
        if err
          return res.status(500).json
            status: 500
            message: "Server Error"
        return res.status(200).json
          status: 200
          message: "Location added succesfully"

