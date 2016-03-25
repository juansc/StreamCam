express = require 'express'
app = express()
bodyParser = require 'body-parser'
db_client = require '../database/database_client'
jwt = require 'jwt-simple'
login = require '../utils/login'
token = require '../utils/token'
videos = require '../utils/videos'


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

app.post '/api/v1/users', login.createNewUser
app.all '/api/v1/users', methodNotAllowed

app.post '/api/v1/authenticate', login.authenticateUser
app.all '/api/v1/authenticate', methodNotAllowed

app.post '/api/v1/videos', videos.createVideo
app.get '/api/v1/videos/:user', videos.getUserVideos
app.all '/api/v1/videos', methodNotAllowed

app.put '/api/v1/manifest/:video_id', videos.appendManifestToVideo

console.log "Magic happens on port #{port}"