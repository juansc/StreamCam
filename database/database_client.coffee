dotenv = require 'dotenv-with-overload'
dotenv.load()

pg = require 'pg'
async = require 'async'
db_string = process.env.DATABASE_URL


db_client = new pg.Client db_string
db_client.connect()

module.exports = db_client
###client.connect()
counter = 0

databaseSetup = () ->
  #client.query "DROP TABLE IF EXISTS users"
  client.query "CREATE TABLE IF NOT EXISTS users(
                id SERIAL PRIMARY KEY,
                username varchar(30) NOT NULL UNIQUE,
                password varchar(30) NOT NULL);"

  query = client.query "INSERT INTO users(username, password)
                values($1, $2)",
                ['foo', 'bar']
  query.on "error", ->
    null

  query = client.query "INSERT INTO users(username, password)
                values($1, $2)",
                ['santa', 'clause']
  query.on "error", ->
    null

  async.series([
    (callback)-> addUser('juanch','coolio',callback)
    (callback)-> addUser('fo','whack',callback)
  ], (err,res) -> client.end())

exports.addUser = (user_name, password, callback) ->
  query = client.query
    text: "INSERT INTO users(username, password) values ($1, $2)"
    values: [user_name, password]

  query.on "error", (err) ->
    console.log "Error inserting values #{user_name} and #{password}
                 into database: \n#{err}"
    callback() if callback?

  query.on "end", (result) ->
    console.log result
    callback(result) if callback?

exports.closeConnection = ->
  client.end()

exports.openConnection = ->
  client.open()

userExists = (user_name) ->
  doesUserExist = false
  query = client.query "SELECT EXISTS(SELECT 1 FROM users
                        WHERE username='#{user_name}')"

  query.on "row", (row, result) ->
    doesUserExist =  row.exists
  query.on "end", ->
    return doesUserExist

databaseSetup()
###
