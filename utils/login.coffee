db_client = require '../database/database_client'
token = require '../utils/token'

exports.authenticateUser = (req, res) ->
  data = req.body
  unless data.user and data.password
    return res.status(400).json
      status: 400
      message: "Please provide a username and password"
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
        message: "Server Error"
    if result.rows[0].exists
      return res.json
        status: 200
        message: "Authentication succeeded."
        token: token.generateToken data.user
    else
      return res.status(404).json
        status: 404
        message: "Account not found."

exports.createNewUser = (req, res) ->
  data = req.body
  username = data.user
  password = data.password

  #console.log "Username is #{username} and password #{password}"
  if password.length < 8
    return res.status(400).json
      status: 400
      message: "Password must have at least eight characters"

  db_client.query
    text: "INSERT INTO users(username, password)
           values ($1, crypt($2,gen_salt('bf',8)))"
    values: [username, password]
  , (err, result) ->
    if err
      if err.code is '23505'
        return res.status(409).json
          status: 409
          message: "Username already exists."
      console.log err
      return res.status(500).json
        status: 500
        message: "Server error."
    res.status(200).json
      status: 200
      message:"Account successfully created."
      token: token.generateToken username