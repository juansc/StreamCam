dotenv = require 'dotenv-with-overload'
dotenv.load()

pg = require 'pg'
async = require 'async'
db_string = process.env.STREAM_CAM_URL


db_client = new pg.Client db_string
db_client.connect()

module.exports = db_client