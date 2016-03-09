express = require 'express'
app = express()
bodyParser = require 'body-parser'
db_client = require '../database/database_client'
token = require '../utils/token'
login = require '../utils/login'

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




app.listen port
console.log "Magic happens on port #{port}"