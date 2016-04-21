token = require '../utils/token'
db_client = require '../database/database_client'
moment = require 'moment'
async = require 'async'

exports.createVideo = (req, res) ->
  user_token = req.body.token or req.headers['x-access-token'] if req
  unless user_token
    return res.status(403).json
      status: 403
      message: "Forbidden action."

  decoded = token.decodeToken user_token
  unless decoded
    return res.status(400).json
      status: 400
      message: "Invalid token."

  user = decoded.user
  video_timestamp = req.body.video_timestamp if req.body.video_timestamp
  console.log video_timestamp
  unless video_timestamp
    video_timestamp = moment().format()

  video_timestamp = moment().format()

  db_client.query
    text: "WITH new_video_id AS (
               INSERT INTO videos(video_date)
               values(timestamp '#{video_timestamp}')
               returning video_id
           ) INSERT INTO user_videos(user_id, video_id)
           values(
               (SELECT user_id FROM users WHERE username='#{user}'),
               (SELECT video_id FROM new_video_id)
           ) returning video_id"
  , (err, results) ->
    if err
      console.log err
      return res.status(500).json
        status: 500
        message: "Server error."
    res.status(200).json
      status: 200
      message:"Video successfully created."
      video_id: results.rows[0].video_id

exports.appendManifestToVideo = (req, res) ->
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

  unless req.body.location
    return res.status(400).json
      status: 400
      message: "No location included"

  location = req.body.location

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
          location.address,
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

exports.getUserVideos = (req, res) ->
  user_token = req.body.token or req.headers['token'] if req
  unless user_token
    return res.status(403).json
      status: 403
      message: "Forbidden action"

  decoded = token.decodeToken user_token
  unless decoded
    return res.status(400).json
      status: 400
      message: "Invalid token"

  unless decoded.user is req.params.user
    return res.status(403).json
      status: 403
      message: "Unauthorized request"

  db_client.query
    text: "SELECT video_id, video_duration, video_date
              FROM
              user_videos INNER JOIN videos USING (video_id)
              INNER JOIN users USING(user_id)
              WHERE username=$1"
    values: [decoded.user]
  , (err, result) ->
    if err
      return res.status(500).json
        status: 500
        message: "Server Error"
    res.status(200).json
      user_videos: result.rows

exports.deleteUserVideo = (req, res) ->
  user_token = req.body.token or req.headers['token'] if req
  unless user_token
    return res.status(403).json
      status: 403
      message: "Forbidden action"

  video_to_delete = req.params.video_id

  unless video_to_delete
    return res.status(400).json
      status: 400
      message: "Video id not specified"

  decoded = token.decodeToken user_token
  unless decoded
    return res.status(400).json
      status: 400
      message: "Invalid token"

  db_client.query
    text: "SELECT EXISTS(
           SELECT 1 FROM
           users INNER JOIN user_videos USING (user_id)
           WHERE video_id=$1 AND username=$2)"
    values: [video_to_delete, decoded.user]
  , (err, result) ->
    if err
      return res.status(500).json
        status: 500
        message: "Server Error"
    if !result.rows[0].exists
      return res.status(403).json
        status: 403
        message: "Unauthorized action."
    else
      async.parallel
        delete_video: (callback) ->
          db_client.query
            text: "DELETE FROM user_videos *
                   WHERE video_id=$1"
            values: [video_to_delete]
          , (err, res) ->
            callback(null, not err)
        delete_manifests: (callback)->
          db_client.query
            text: "DELETE FROM video_manifests *
                   WHERE video_id=$1"
            values: [video_to_delete]
          , (err, res) ->
            callback(null, not err)
      , (err, result) ->
        if result.delete_video and result.delete_manifests
          return res.status(200).json
            status: 200
            message: "Video deleted"
        else
          return res.status(500).json
            status: 500
            message: "Server Error"
