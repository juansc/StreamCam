express = require 'express'
app = express()
cors = require('cors')
bodyParser = require 'body-parser'
db_client = require '../database/database_client'
jwt = require 'jwt-simple'
login = require '../utils/login'
token = require '../utils/token'
video_routes = require '../utils/video_routes'


app.use bodyParser.urlencoded {extended: true}
app.use bodyParser.json()
app.use cors()

port = process.env.PORT || 3000

router = express.Router()


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

app.post '/api/v1/videos', video_routes.createVideo
app.get '/api/v1/videos/:user', video_routes.getUserVideos
app.post '/api/v1/videos/:video_id', video_routes.closeUserVideo
app.delete '/api/v1/videos/:video_id', video_routes.deleteUserVideo
app.all '/api/v1/videos', methodNotAllowed

app.get '/api/v1/manifest/:video_id', video_routes.getVideoManifest
app.put '/api/v1/manifest/:video_id', video_routes.appendManifestToVideo

app.listen port

console.log "Currently using #{port}"