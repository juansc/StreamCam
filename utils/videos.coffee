token = require '../utils/token'
db_client = require '../database/database_client'
moment = require 'moment'
async = require 'async'
fs = require 'fs'
res_builder = require '../utils/res_builder'

###
TODO:
After we close the video we must make the manifest.
After we close the video we must tell Wowza to stop recording.
###
exports.createVideo = (req, res) ->
  user_token = req.body.token or req.headers['x-access-token'] if req
  return res_builder.UnauthorizedActionResponse res unless user_token

  decoded = token.decodeToken user_token
  return res_builder.invalidTokenResponse res unless decoded

  user = decoded.user

  video_timestamp = if req.body.video_timestamp then req.body.video_timestamp else moment().format()

  async.waterfall [
    async.apply(createNewVideo, user, video_timestamp, res)
    createAndInsertVideoFileNames
  ], (err, video_id) ->
    if err
      res = err
    else
      res = res.status(200).json
        status: 200
        message:"Video successfully created."
        video_id: video_id
    return res

createNewVideo = (user, timestamp, res, callback) ->
  db_client.query
    text: "WITH new_video_id AS (
               INSERT INTO videos(video_date)
               values(timestamp '#{timestamp}')
               returning video_id
           ) INSERT INTO user_videos(user_id, video_id)
           values(
               (SELECT user_id FROM users WHERE username='#{user}'),
               (SELECT video_id FROM new_video_id)
           ) returning video_id"
  , (db_error, results) ->
    err = null
    console.log db_error if db_error
    err = res_builder.serverErrorResponse res if db_error
    callback err, results.rows[0].video_id, res

createAndInsertVideoFileNames = (video_id, res, callback) ->
  id_prefix_length = 10
  file_name = generateRandomID(id_prefix_length) + "_#{video_id}"
  db_client.query
    text: "UPDATE videos
           SET file_name='#{file_name}'
           WHERE video_id=$1"
    values: [video_id]
  , (db_error, results) ->
    console.log db_error if db_error
    err = null
    err = res_builder.serverErrorResponse res if db_error
    callback err, video_id, file_name, res

exports.closeUserVideo = (req, res) ->
  user_token = req.body.token or req.headers['x-access-token'] if req
  return res_builder.UnauthorizedActionResponse res unless user_token

  decoded = token.decodeToken user_token
  return res_builder.invalidTokenResponse res unless decoded

  video_id = req.params.video_id
  return res_builder.unspecifiedVideoResponse res unless video_id

  async.waterfall [
    async.apply(videoBelongsToUser, decoded.user, video_id, res),
    async.apply(closeVideo, video_id, res)
    async.apply(getFileName, video_id, res)
    makeManifest
    # TODO
    #async.apply(closeVideoStream, video_id, res),
  ], (err, result) ->
    if err
      res = err
    else
      res = res.status(200).json
        status: 200
        message: "Video succesfully closed."
    return res

exports.appendManifestToVideo = (req, res) ->
  user_token = req.body.token or req.headers['x-access-token'] if req
  return res_builder.UnauthorizedActionResponse res unless user_token

  decoded = token.decodeToken user_token
  return res_builder.invalidTokenResponse res unless decoded

  location = req.body.location
  return res_builder.noLocationIncludedResponse res unless location

  video_id = req.params.video_id
  return res_builder.unspecifiedVideoResponse err unless video_id

  async.waterfall [
    async.apply(videoBelongsToUser, decoded.user, video_id, res),
    async.apply(insertLocationIntoManifest, video_id, location, res)
  ], (err, result) ->
    if err
      res = err
    else
      res = res.status(200).json
        status: 200
        message: "Location added succesfully"
    return res

exports.getUserVideos = (req, res) ->
  user_token = req.body.token or req.headers['token'] if req
  return res_builder.UnauthorizedActionResponse res unless user_token

  decoded = token.decodeToken user_token
  return res_builder.invalidTokenResponse res unless decoded

  unless decoded.user is req.params.user
    return res_builder.UnauthorizedActionResponse res

  db_client.query
    text: "SELECT
           video_id, video_duration, video_date,
           video_file, manifest_file
           FROM
           user_videos INNER JOIN videos USING (video_id)
           INNER JOIN users USING(user_id)
           WHERE username=$1
           AND video_state='closed'"
    values: [decoded.user]
  , (err, result) ->
    return res_builder.serverErrorResponse res if err
    res.status(200).json
      user_videos: result.rows

exports.deleteUserVideo = (req, res) ->
  user_token = req.body.token or req.headers['token'] if req
  return res_builder.UnauthorizedActionResponse res unless user_token

  video_to_delete = req.params.video_id
  return res_builder.unspecifiedVideoResponse err unless video_to_delete

  decoded = token.decodeToken user_token
  return res_builder.invalidTokenResponse res unless decoded

  async.waterfall [
    async.apply(videoBelongsToUser, decoded.user, video_to_delete, res),
    async.apply(deleteVideo, video_to_delete, res),
    async.apply(deleteVideoManifests, video_to_delete, res)
  ], (err, result)->
    if err
      res = err
    else
      res = res.status(200).json
        status: 200
        message: "Video deleted"
    return res

videoBelongsToUser = (user, video_id, res, callback) ->
  db_client.query
    text: "SELECT EXISTS(
           SELECT 1 FROM
           users INNER JOIN user_videos USING (user_id)
           WHERE video_id=$1 AND username=$2)"
    values: [video_id, user]
    , (db_error, result) ->
      err = null

      if db_error
        err = res_builder.serverErrorResponse res
      else if !result.rows[0].exists
        err = res_builder.UnauthorizedActionResponse res

      callback err

deleteVideoManifests = (video_id, res, callback) ->
  db_client.query
    text: "DELETE FROM video_manifests *
           WHERE video_id=$1"
    values: [video_id]
  , (db_error, res) ->
    err = if db_error then res_builder.serverErrorResponse res else null
    callback err

deleteVideo = (video_id, res, callback) ->
  db_client.query
    text: "DELETE FROM user_videos *
           WHERE video_id=$1"
    values: [video_id]
  , (db_error, results) ->
    err = if db_error then res_builder.serverErrorResponse res else null
    callback err

insertLocationIntoManifest = (video_id, location, res, callback) ->
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
  , (db_error, result) ->
    console.log db_error if db_error
    err = if db_error then res_builder.serverErrorResponse res else null
    callback err

 closeVideo = (video_id, res, callback) ->
  db_client.query
    text: "UPDATE videos
           SET video_state='closed'
           WHERE video_id=$1"
    values: [video_id]
  , (db_error, result) ->
    console.log db_error if db_error
    err = if db_error then res_builder.serverErrorResponse res else null
    callback err

getFileName = (video_id, res, callback) ->
  db_client.query
    text: "SELECT file_name FROM videos WHERE video_id=$1"
    values: [video_id]
  , (db_error, result) ->
    console.log db_error if db_error
    err = if db_error then res_builder.serverErrorResponse res else null
    callback err, video_id, result.rows[0].file_name, res

makeManifest = (video_id, file_name, res, callback) ->
  manifest_file_name = "#{file_name}.txt"

  db_client.query
    text: "SELECT
           location_timestamp,
           address,
           latitude,
           longitude
           FROM video_manifests WHERE video_id=$1"
    values: [video_id]
    , (db_error, result) ->
      console.log db_error if db_error
      callback res_builder.serverErrorResponse res if db_error

      if result.rows.length is 0
        manifest = "No locations for video #{video_id}"
      else
        manifest = "Locations for video #{video_id}\n\n"
        for row in result.rows
          manifest += "Time: #{row.location_timestamp}
                          Address: #{row.address}
                          Long: #{row.longitude}
                          Lat: #{row.latitude}\n\n"

      fs.writeFile manifest_file_name, manifest, (write_err, data) ->
        console.log write_err if write_err
        err = if write_err then res_builder.serverErrorResponse res else null
        callback err, video_id



generateRandomID = (n) ->
  chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  chars_length = chars.length
  id = ""
  for ind in [1..n]
    id += chars[Math.floor(Math.random()*chars_length)]
  id
