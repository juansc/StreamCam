async = require 'async'
request = require 'supertest'
expect = require('chai').expect
url = 'http://localhost:3000'
db_client = require '../database/database_client'
moment = require 'moment'

describe 'StreamCam API', ->

  describe 'If no token is provided for authorized routes', ->
    it 'creating a video returns a 403', (done) ->
      request url
            .post '/api/v1/videos'
            .send {video_timestamp: '2015-12-12'}
            .expect 403, done
    it 'appending to a manifest returns a 403', (done) ->
      request url
            .put '/api/v1/manifest/12'
            .send
              video_id: 69,
              location:
                timestamp: '2015-03-01'
                address: 'My Address'
                latitude: 12.0
                longitude: 12.0
            .expect 403, done
    #it 'asking for list of videos returns a 403', ->
    #it 'closing a video returns a 403', ->

  describe 'Login', ->
    describe 'when credentials are correct', ->
      it 'user gets a 200 and a token is given', (done)->
        request url
          .post '/api/v1/authenticate'
          .send {user:'Juan', password: 'LizzyLizzy'}
          .expect 200
          .end (err,res) ->
            expect(res.body.token).to.not.be.null
            expect(res.body.token).to.match(/^[\w\-]*\.[\w\-]*\.[\w\-]*$/)
            done()
    describe 'if the credentials are invalid', ->
      it 'user gets a 404', (done) ->
        request url
          .post '/api/v1/authenticate'
          .send {user:'foo', password: 'bar'}
          .expect 404, done
    describe 'if the password is incorrect', ->
      it 'user gets a 401', (done) ->
        request url
          .post '/api/v1/authenticate'
          .send {user:'Juan', password: 'mypassword'}
          .end (err, res) ->
            expect(res.body.status).to.equal(401)
            done()

  describe 'Create New Account', ->
    describe 'if the credenials are available', ->
      it 'the user gets a 200 and a token', (done) ->
        request url
          .post '/api/v1/users'
          .send {user: 'Juanchito', password:'mypassword'}
          .end (err, res) ->
            expect(res.statusCode).to.equal(200)
            expect(res.body).to.have.property('token')
            expect(res.body.token).to.match(/^[\w\-]*\.[\w\-]*\.[\w\-]*$/)
            done()

    describe 'if the username is already taken', ->
      it 'user gets a 409', (done) ->
        request url
          .post '/api/v1/users'
          .send {user: 'Juan', password:'mypassword'}
          .expect 409, done

  describe 'Creating a video', ->
    describe 'when a user wants to create a new video', ->
      current_date = null
      number_of_videos = 0

      before ->
        current_date = moment().format()
        db_client.query
          text: "SELECT COUNT(*) FROM user_videos"
        , (err, result) ->
          number_of_videos = +result.rows[0].count

      it 'it returns a 200', (done) ->
        request url
        .post '/api/v1/authenticate'
        .send {user:'Juan', password: 'LizzyLizzy'}
        .end (err,res) ->
          token = res.body.token
          request url
            .post '/api/v1/videos'
            .send {token, video_timestamp: current_date}
            .expect 200, done
      it 'creates a new video', (done) ->
        db_client.query
          text: "SELECT COUNT(*) FROM user_videos"
        ,(err, result) ->
          new_number_of_videos = +result.rows[0].count
          expect(new_number_of_videos).to.equal(number_of_videos + 1)
          done()

  describe 'Adding to Manifests', ->
    describe 'if the request is good', ->
      timestamp = '2015-10-10'

      it 'user gets a 200', (done)->
        request url
          .post '/api/v1/authenticate'
          .send {user:'Juan', password: 'LizzyLizzy'}
          .end (err,res) ->
            token = res.body.token
            request url
              .put '/api/v1/manifest/1'
              .send {
                token
                location: JSON.stringify
                  address: '1 LMU Drive'
                  timestamp: timestamp
                  latitude: 2
                  longitude: 4
              }
              .expect 200, done
      it 'location is added succesfully', ->
        db_client.query
          text: "SELECT EXISTS(
                  SELECT * FROM video_manifests
                  WHERE
                  video_id=1
                  AND
                  location_timestamp=(date '2015-10-10')
                  AND
                  address='1 LMU Drive'
                  AND
                  latitude=2
                  AND
                  longitude=3)"
        ,(err, result) ->
          expect(result.rows[0].exists).to.equal(true)
          done()
    describe 'if the user does not provide a location', ->
      it 'the user gets a 400', ->
    describe 'if the user does not own the video', ->
      it 'the user receives a 403', ->
    describe 'if the location cannot be parsed', ->
      it 'the user receeves a 400', ->