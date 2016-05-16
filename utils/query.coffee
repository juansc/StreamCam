db_client = require '../database/database_client'
res_builder = require '../utils/res_builder'

exports.createNewVideo = (user, timestamp, res, callback) ->
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
    if db_error
      err = res_builder.serverErrorResponse res
      callback err
    else
      callback err, results.rows[0].video_id, res

exports.createAndInsertVideoFileNames = (video_id, res, callback) ->
  id_prefix_length = 20
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

exports.videoBelongsToUser = (user, video_id, res, callback) ->
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

exports.deleteVideoManifests = (video_id, res, callback) ->
  db_client.query
    text: "DELETE FROM video_manifests *
           WHERE video_id=$1"
    values: [video_id]
  , (db_error, res) ->
    err = if db_error then res_builder.serverErrorResponse res else null
    callback err

exports.deleteVideo = (video_id, res, callback) ->
  db_client.query
    text: "DELETE FROM user_videos *
           WHERE video_id=$1"
    values: [video_id]
  , (db_error, results) ->
    err = if db_error then res_builder.serverErrorResponse res else null
    callback err

exports.insertLocationIntoManifest = (video_id, location, res, callback) ->
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

exports.closeVideo = (video_id, res, callback) ->
  db_client.query
    text: "UPDATE videos
           SET video_state='closed'
           WHERE video_id=$1"
    values: [video_id]
  , (db_error, result) ->
    console.log db_error if db_error
    err = if db_error then res_builder.serverErrorResponse res else null
    callback err

exports.getFileName = (video_id, res, callback) ->
  db_client.query
    text: "SELECT file_name FROM videos WHERE video_id=$1"
    values: [video_id]
  , (db_error, result) ->
    console.log db_error if db_error
    err = null
    if db_error
      err = res_builder.serverErrorResponse res
      callback err
    else
      callback err, video_id, result.rows[0].file_name, res

generateRandomID = (n) ->
  chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  chars_length = chars.length
  id = ""
  for ind in [1..n]
    id += chars[Math.floor(Math.random()*chars_length)]
  id