token = require '../utils/token'
db_client = require '../database/database_client'
moment = require 'moment'
async = require 'async'
query = require '../utils/query'
fs = require 'fs'
res_builder = require '../utils/res_builder'

exports.createVideo = (req, res) ->
  user_token = req.body.token or req.headers['x-access-token'] if req
  return res_builder.UnauthorizedActionResponse res unless user_token

  decoded = token.decodeToken user_token
  return res_builder.invalidTokenResponse res unless decoded

  user = decoded.user

  video_timestamp = if req.body.video_timestamp then req.body.video_timestamp else moment().format()

  async.waterfall [
    async.apply query.createNewVideo, user, video_timestamp, res
    query.createAndInsertVideoFileNames
  ], (err, video_id, file_name) ->
    if err
      res = err
    else
      res = res.status(200).json
        status: 200
        message:"Video successfully created."
        video_id: video_id
        file_name: file_name
    return res

exports.closeUserVideo = (req, res) ->
  user_token = req.body.token or req.headers['x-access-token'] if req
  return res_builder.UnauthorizedActionResponse res unless user_token

  decoded = token.decodeToken user_token
  return res_builder.invalidTokenResponse res unless decoded

  video_id = req.params.video_id
  return res_builder.unspecifiedVideoResponse res unless video_id

  async.waterfall [
    async.apply query.videoBelongsToUser, decoded.user, video_id, res
    async.apply query.closeVideo, video_id, res
    async.apply query.getFileName, video_id, res
    makeManifest
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
  return res_builder.unspecifiedVideoResponse res unless video_id

  async.waterfall [
    async.apply query.videoBelongsToUser, decoded.user, video_id, res
    async.apply query.insertLocationIntoManifest, video_id, location, res
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
           video_id, video_date, file_name
           FROM
           user_videos INNER JOIN videos USING (video_id)
           INNER JOIN users USING(user_id)
           WHERE username=$1
           AND video_state='closed'"
    values: [decoded.user]
  , (err, result) ->
    console.log err if err
    return res_builder.serverErrorResponse res if err
    res.status(200).json
      user_videos: result.rows

exports.deleteUserVideo = (req, res) ->
  user_token = req.body.token or req.headers['token'] if req
  return res_builder.UnauthorizedActionResponse res unless user_token

  video_to_delete = req.params.video_id
  return res_builder.unspecifiedVideoResponse res unless video_to_delete

  decoded = token.decodeToken user_token
  return res_builder.invalidTokenResponse res unless decoded

  async.waterfall [
    async.apply query.videoBelongsToUser, decoded.user, video_to_delete, res
    async.apply query.deleteVideo, video_to_delete, res
    async.apply query.deleteVideoManifests, video_to_delete, res
  ], (err, result)->
    if err
      res = err
    else
      res = res.status(200).json
        status: 200
        message: "Video deleted"
    return res

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

exports.getVideoManifest = (req, res) ->
  user_token = req.body.token or req.headers['token'] if req
  return res_builder.UnauthorizedActionResponse res unless user_token

  video_id = req.params.video_id
  return res_builder.unspecifiedVideoResponse res unless video_id

  decoded = token.decodeToken user_token
  return res_builder.invalidTokenResponse res unless decoded

  async.waterfall [
    async.apply query.videoBelongsToUser, decoded.user, video_id, res
    async.apply query.getFileName, video_id, res
  ], (err, result)->
    if err
      res = err
    else
      fs.access manifest_file, fs.F_OK, (access_err) ->
        console.log access_err if access_err
        if access_err
          return callback res_builder.serverErrorResponse res
        else
          res.download manifest_file_name, (err) ->
            console.log err if err
    return res


