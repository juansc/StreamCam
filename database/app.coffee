pg = require 'pg'
conString = 'pg://localhost:5432/todo'

client = new pg.Client conString
client.connect()

client.query "CREATE TABLE IF NOT EXISTS emps(firstname varchar(64),
              lastname varchar(64))"
client.query "INSERT INTO emps(firstname, lastname) values($1, $2)",
  ['Ronald', 'McDonald']
client.query "INSERT INTO emps(firstname, lastname) values($1, $2)",
  ['Mayor', 'McCheese']

query = client.query "SELECT firstname, lastname FROM emps ORDER BY
                      lastname, firstname"
query.on "row", (row, result) ->
  result.addRow(row)

query.on "end", (result) ->
  console.log(JSON.stringify(result.rows, null, "    "))
  client.end()

