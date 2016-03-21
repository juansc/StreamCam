token = require '../utils/token'
db_client = require '../database/database_client'

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
  video_timestamp = req.body.video_timestamp

  db_client.query
    text: "WITH new_video_id AS (
               INSERT INTO videos(video_date)
               values(date '#{video_timestamp}')
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