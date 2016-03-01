express = require 'express'
app = express()
bodyParser = require 'body-parser'
db_client = require '../database/database_client'

app.use bodyParser.urlencoded {extended: true}
app.use bodyParser.json()

port = process.env.PORT || 3000

router = express.Router()

# All our routes will be here
router.get '/', (req, res) ->
  res.json {message: "hooray! Welcome to our api!"}

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
    text: "INSERT INTO users(username, password) values ($1, $2)"
    values: [username, password]
  , (err, result) ->
    if err
      if err.code is '23505'
        return res.json {message: "Insertion failed! Reasons: Username already exists!"}
      return res.json {message: "Insertion failed!"}
    res.json {message:"Insertion was successful"}

app.all '/api/v1/users', (req,res) ->
  res.status(405).json 'message' : 'method not allowed'

app.listen port
console.log "Magic happens on port #{port}"