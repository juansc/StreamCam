express = require 'express'
app = express()
bodyParser = require 'body-parser'
db_client = require '../database/database_client'
login = require '../utils/login'
jwt = require 'jwt-simple'

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





app.listen port
console.log "Magic happens on port #{port}"