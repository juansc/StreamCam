jwt = require 'jwt-simple'
moment = require 'moment'
jwt_token_secret = process.env.JWT_STRING

default_token_duration = moment.duration 7, 'days'

exports.generateToken = (iss) ->
  expires = moment().add(default_token_duration).valueOf()
  token = jwt.encode {iss, expires}, jwt_token_secret
