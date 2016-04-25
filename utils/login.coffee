db_client = require '../database/database_client'
token = require '../utils/token'
error_builder = require '../utils/error_builder'

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
           )"
    values: [data.user]
  , (err, result) ->
    return error_builder.serverErrorResponse res if err

    if not result.rows[0].exists
      return res.status(404).json
        status: 404
        message: "Username not found"
    else
      db_client.query
        text: "SELECT EXISTS(
               SELECT 1 FROM users
               WHERE username=$1
               AND
               password=crypt($2, password)
               )"
        values: [data.user, data.password]
      , (err, result) ->
        return error_builder.serverErrorResponse res if err

        if result.rows[0].exists
          return res.status(200).json
            status: 200
            message: "Authentication succeeded."
            token: token.generateToken data.user
        else
          return res.status(401).json
            status: 401
            message: "Incorrect password."

exports.createNewUser = (req, res) ->
  data = req.body
  username = data.user
  password = data.password

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
        return error_builder.userAlreadyExistsResponse res

      return error_builder.serverErrorResponse res
    res.status(200).json
      status: 200
      message:"Account successfully created."
      token: token.generateToken username