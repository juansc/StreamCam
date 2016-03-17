request = require 'supertest'
expect = require('chai').expect
url = 'http://localhost:3000'
db_client = require '../database/database_client'

describe 'StreamCam API', ->
  before ->
  console.log "This will run before all the tests!!"

  after ->
    console.log "All tests should've finished by now!"

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

  # TODO: Implement scenario where username is
  #       correct but password is wrong.
    describe 'if the password is incorrect', ->
      it 'user gets a 401', (done) ->
        request url
          .post '/api/v1/authenticate'
          .send {user:'Juan', password: 'mypassword'}
          .expect 403, done


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

  describe 'Adding to Manifests', ->
    describe 'if the request is good', ->
      it 'user gets a 200', ->
      it 'location is added succesfully', ->
    describe 'if the user does not provide a location', ->
      it 'the user gets a 400', ->
    describe 'if the user does not own the video', ->
      it 'the user receives a 403', ->
    describe 'if the location cannot be parsed', ->
      it 'the user receeves a 400', ->