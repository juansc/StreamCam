express = require 'express'
app = express()
bodyParser = require 'body-parser'

app.use bodyParser.urlencoded {extended: true}
app.use bodyParser.json()

port = process.env.PORT || 3000

router = express.Router()

# All our routes will be here
router.get '/', (req, res) ->
  res.json {message: "hooray! Welcome to our api!"}

# All the routes will be prefixed with /api
app.use '/api', router

app.listen port
console.log "Magic happens on port #{port}"