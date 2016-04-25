exports.serverErrorResponse = (res) ->
  res.status(500).json
    status: 500
    message: "Server error."

exports.invalidTokenResponse = (res) ->
  res.status(400).json
    status: 400
    message: "Invalid token."

exports.forbiddenActionResponse = (res) ->
  res.status(403).json
    status: 403
    message: "Forbidden action"

exports.UnauthorizedActionResponse = (res) ->
  res.status(403).json
    status: 403
    message: "Unauthorized action."

exports.noLocationIncludedResponse = (res) ->
  res.status(400).json
    status: 400
    message: "No location included"

exports.unspecifiedVideoResponse = (res) ->
  res.status(400).json
    status: 400
    message: "Video id not specified"

exports.userAlreadyExistsResponse = (res) ->
  res.status(409).json
    status: 409
    message: "Username already exists."

exports.incorrectPasswordResponse = (res) ->
  res.status(401).json
    status: 401
    message: "Incorrect password."

exports.improperLoginResponse = (res) ->
  res.status(400).json
    status: 400
    message: "Please provide a username and password"

exports.usernameNotFoundResponse = (res) ->
  res.status(404).json
    status: 404
    message: "Username not found"
