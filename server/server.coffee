express = require 'express'
app = express()
bodyParser = require 'body-parser'
db_client = require '../database/database_client'
login = require '../utils/login'
jwt = require 'jwt-simple'
token = require '../utils/token'

app.use bodyParser.urlencoded {extended: true}
app.use bodyParser.json()

port = process.env.PORT || 3000

router = express.Router()


# All our routes will be here

methodNotAllowed = (req, res) ->
  res.status(405).json
    status: 405
    message: 'Method not allowed'


app.get '/', (req, res) ->
  res.status(200).json
    status: 200
    message: "hooray! Welcome to our api!"
  console.log "Someone hit the api"

# We can only post to this one
app.post '/api/v1/users', login.createNewUser
app.all '/api/v1/users', methodNotAllowed

app.post '/api/v1/authenticate', login.authenticateUser

app.put '/api/v1/manifest/:video_id', (req, res) ->
  console.log req
  user_token = req.body.access_token
  if user_token
    try
      # ...
      decoded = jwt.decode user_token, process.env.JWT_STRING
      console.log decoded
    catch err
      # ...

app.post '/api/v1/videos', (req, res) ->
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

app.listen port
console.log "Magic happens on port #{port}"