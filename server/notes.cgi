#! /usr/bin/python

#Import modules for CGI handling 
import cgi, sys, json, urllib,  time, os, uuid, cgitb, sqlite3
from datetime import datetime

#cgitb.enable(logdir = "")
cgitb.enable()

#Open database connection
#connection = MySQLdb.connect (host = "localhost", user = "cp341mobile", passwd = "blueFoxMoon", db = "ereca")

#initialize cursor
#cursor = connection.cursor()

#table name
tableName = "appresults"



#list of column names in the table, also these are the keys 
columnNames = [
	("downSpeed", "REAL"), 
	("bytesIn", "INTEGER"), 
	("latitude","REAL"), 
	("longitude","REAL"), 
	("altitude","REAL"), 
	("locationProvider", "TEXT"),
	("timeStamp", "TEXT"),
	("timeStampFmt", "TEXT"),
	("MACAddr", "TEXT" ),
	("network", "TEXT" ),
	("connectionTime", "REAL"),
	("connectionTimeUnit", "TEXT"),
	("fileDownloaded", "TEXT"),
	("wirelessAccessPointMAC", "TEXT"),
	("uuid", "TEXT PRIMARY KEY")
	]
	
#attempt to execute a statement, write errors to output.txt
def executeSqliteStatement( statement, insertTuple ):
	try:
		#execute and commit
		c.execute( statement, insertTuple )
		db.commit()
		return True
	except sqlite3.Error as er:
		f.write("ERROR WITH SQLITE3 \n" )
		f.write( er.message )
		f.write("statement: " + statement)
		f.write( '\n' )
		return False



db = sqlite3.connect("tiger_test_db.db")
c = db.cursor()

#make a statement to create a table if it doesn't already exist
createTableStatement = "CREATE TABLE IF NOT EXISTS " + tableName + "("
for name in columnNames:
	createTableStatement += name[0] + " " + name[1] + ", "
createTableStatement = createTableStatement[:-2]
createTableStatement += ")"
c.execute( createTableStatement )

#boolean flag for a bad http request
flag = False;

json_data = None
decoded_data = None
fields = None

# Create instance of FieldStorage 
try:
	form = cgi.FieldStorage()
	#fields = form.getlist()
	#data = form.value
except:
	print "Content-type:text/html\r\n\r\n"
	print "This cgi script is only for the Android app TigerTest."
	
f = open( "output.txt", 'w' )
f.write( str( datetime.now() ) )
f.write( "\n" )

if form:
	for key in form:
		f.write( str(key) + " : " + str(form[key].value ) + "\n")
		
	#if there is a uuid
	if "uuid" in form:
		uuid = str( form["uuid"].value )
		
			#c.execute( "SELECT * FROM " + tableName + " WHERE uuid = ?", (uuid,))
		executeSqliteStatement( "SELECT * FROM " + tableName + " WHERE uuid = ?", (uuid,) )
		if len( c.fetchall() ) != 0:
			f.write("uuid already exists in system!")
			#here we should return a message to a client saying we already have that record

		else:

			#begin an insert statement
			insertStatement = "INSERT INTO " + tableName + "("

			# #state the columns to insert into
			for name in columnNames:
				if name[0] in form:
					insertStatement += name[0] + ", "
				else:
					f.write("\n Not Present: " + name[0] + "\n")
			insertStatement = insertStatement[:-2]
			insertStatement += ") VALUES("

			#put in values
			insertTuple = ()
			for name in columnNames:
	
				#get data if possible from form
				if name[0] in form:
					insertStatement += "?, " 
					#concat onto tuple
					insertTuple += (str(form[name[0]].value),)


			insertStatement = insertStatement[:-2]
			insertStatement += ")"

			#attempt to execute statement
			if executeSqliteStatement( insertStatement, insertTuple ):

				print "Content-type:text/html\r\n\r\n"
				print "{added:" + uuid + "}"
			else:
				print "Content-type:text/html\r\n\r\n"
				print "{added:" + uuid + "}"
				

				


			f.write("executed insert statement: \n")	
			f.write(insertStatement)
			f.write( '\n' )
	

	
			print "Content-type:text/html\r\n\r\n"
			print insertStatement
			
			#return a 201 message to the client saying we successfully posted the record
	
	#if there is no uuid
	else:
		f.write("ERROR no uuid")
	
f.close()



print "Content-type:text/html\r\n\r\n"
print "Got the note!"








