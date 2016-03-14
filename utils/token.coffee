jwt = require 'jwt-simple'
moment = require 'moment'
jwt_token_secret = process.env.JWT_STRING

default_token_duration = moment.duration 7, 'days'

exports.generateToken = (user) ->
  expires = moment().add(default_token_duration).valueOf()
  token = jwt.encode {user, expires}, jwt_token_secret

exports.decodeToken = (user_token) ->
  try
    decoded = jwt.decode user_token, jwt_token_secret
  catch err
    console.log "Token decoding error occurred: \n#{err}"
    null


