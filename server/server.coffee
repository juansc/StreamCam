express = require 'express'
app = express()
bodyParser = require 'body-parser'
db_client = require '../database/database_client'

app.use bodyParser.urlencoded {extended: true}
app.use bodyParser.json()

port = process.env.PORT || 3000

router = express.Router()

# All our routes will be here
app.get '/', (req, res) ->
  res.status(200).json
    status: 200
    message: "hooray! Welcome to our api!"
  console.log "Someone hit the api"

# All the routes will be prefixed with /api

# We can only post to this one
app.post '/api/v1/users', (req, res) ->
  data = req.body
  username = data.user
  password = data.password
  console.log "Username is #{username} and password #{password}"
  if password.length < 8
    return res.json {message: "Password must have at least eight characters"}

  db_client.query
    text: "INSERT INTO users(username, password)
           values ($1, crypt($2,gen_salt('bf',8)))"
    values: [username, password]
  , (err, result) ->
    if err
      if err.code is '23505'
        return res.status(409).json
          status: 409
          message: "Username already exists"
      console.log err
      return res.status(500).json
        status: 500
        message: "Unknown error occurred"
    res.status(200).json
      status: 200
      message:"Insertion was successful"



app.all '/api/v1/users', (req,res) ->
  res.status(405).json
    status: 405
    message: 'method not allowed'

app.post '/api/v1/authenticate', (req, res) ->
  data = req.body
  unless data.user and data.password
    return res.status().json {message: "Please provide a username and password"}
  db_client.query
    text: "SELECT EXISTS(
           SELECT 1 FROM users
           WHERE username=$1
           AND
           password=crypt($2, password)
           )"
    values: [data.user, data.password]
  , (err, result) ->
    if err
      return res.json
        status: 500
        message: "Some error occurred..."
    if result.rows[0].exists
      return res.json
        status: 200
        message: "You are in the database!!!"
    else
      return res.json
        status: 404
        message: "You are not in the database!!!"

app.listen port
console.log "Magic happens on port #{port}"