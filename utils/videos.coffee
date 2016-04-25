token = require '../utils/token'
db_client = require '../database/database_client'
moment = require 'moment'
async = require 'async'
fs = require 'fs'
error_builder = require '../utils/error_builder'

exports.createVideo = (req, res) ->
  user_token = req.body.token or req.headers['x-access-token'] if req

  return error_builder.UnauthorizedActionResponse res unless user_token

  decoded = token.decodeToken user_token
  return error_builder.invalidTokenResponse res unless decoded

  user = decoded.user
  video_timestamp = req.body.video_timestamp if req.body.video_timestamp

  unless video_timestamp
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
      return error_builder.serverErrorResponse res
    res.status(200).json
      status: 200
      message:"Video successfully created."
      video_id: results.rows[0].video_id

exports.appendManifestToVideo = (req, res) ->
  user_token = req.body.token or req.headers['x-access-token'] if req

  return error_builder.UnauthorizedActionResponse res unless user_token

  decoded = token.decodeToken user_token

  return error_builder.invalidTokenResponse res unless decoded

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
    return error_builder.serverErrorResponse res if err

    if !result.rows[0].exists
      return error_builder.UnauthorizedActionResponse err
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
        return error_builder.serverErrorResponse res if err

        res.status(200).json
          status: 200
          message: "Location added succesfully"

exports.getUserVideos = (req, res) ->
  user_token = req.body.token or req.headers['token'] if req

  return error_builder.UnauthorizedActionResponse res unless user_token

  decoded = token.decodeToken user_token

  return error_builder.invalidTokenResponse res unless decoded

  unless decoded.user is req.params.user
    return error_builder.UnauthorizedActionResponse res

  db_client.query
    text: "SELECT video_id, video_duration, video_date
              FROM
              user_videos INNER JOIN videos USING (video_id)
              INNER JOIN users USING(user_id)
              WHERE username=$1"
    values: [decoded.user]
  , (err, result) ->
    return error_builder.serverErrorResponse res if err
    res.status(200).json
      user_videos: result.rows

exports.deleteUserVideo = (req, res) ->
  user_token = req.body.token or req.headers['token'] if req

  return error_builder.UnauthorizedActionResponse res unless user_token

  video_to_delete = req.params.video_id

  unless video_to_delete
    return res.status(400).json
      status: 400
      message: "Video id not specified"

  decoded = token.decodeToken user_token
  return error_builder.invalidTokenResponse res unless decoded

  db_client.query
    text: "SELECT EXISTS(
           SELECT 1 FROM
           users INNER JOIN user_videos USING (user_id)
           WHERE video_id=$1 AND username=$2)"
    values: [video_to_delete, decoded.user]
  , (err, result) ->
    return error_builder.serverErrorResponse res if err

    if !result.rows[0].exists
      return error_builder.UnauthorizedActionResponse res
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
          return error_builder.serverErrorResponse res

exports.getVideoManifest = (req, res) ->
  user_token = req.body.token or req.headers['token'] if req

  return exports.UnauthorizedActionResponse err unless user_token

  video_id = req.params.video_id

  unless video_id
    return res.status(400).json
      status: 400
      message: "Video id not specified"

  decoded = token.decodeToken user_token
  return error_builder.invalidTokenResponse res unless decoded

  db_client.query
    text: "SELECT EXISTS(
           SELECT 1 FROM
           users INNER JOIN user_videos USING (user_id)
           WHERE video_id=$1 AND username=$2)"
    values: [video_id, decoded.user]
  , (err, result) ->
    return error_builder.serverErrorResponse res if err

    if !result.rows[0].exists
      return error_builder.UnauthorizedActionResponse res
    else
      db_client.query
        text: "SELECT
               location_timestamp,
               address,
               latitude,
               longitude
               FROM video_manifests WHERE video_id=$1;"
        values: [video_id]
        , (err, result) ->
          return error_builder.serverErrorResponse res if err
          if result.rows.length is 0
            manifest = "No locations for video #{video_id}"
          else
            manifest = "Locations for video #{video_id}\n\n"
            for row in result.rows
              manifest_row = "Time: #{row.location_timestamp}
                              Address: #{row.address}
                              Long: #{row.longitude}
                              Lat: #{row.latitude}"
              manifest += manifest_row + "\n\n"

          manifest_file_name = "#{video_id}_manifest.txt"
          fs.writeFile manifest_file_name, manifest, (err, data) ->
            return error_builder.serverErrorResponse res if err

            res.download manifest_file_name, (err) ->
              fs.unlink manifest_file_name