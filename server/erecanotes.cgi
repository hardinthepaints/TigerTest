#! /usr/bin/python

'''
CP341 Mobile Computing
Jay Batavia
Edited: Natalie Browning 2/3 -- added add and get user information (username + password)
'''

# Import modules for CGI handling 
import cgi, cgitb, sys, json, urllib, MySQLdb, uuid, time, os, urlparse

#cgitb.enable(display=0, logdir="./ereca_log")

#Open database connection
connection = MySQLdb.connect (host = "localhost", user = "cp341mobile", passwd = "blueFoxMoon", db = "ereca")

note_limit = 100

#initialize cursor
cursor = connection.cursor()

pic_dir = "./ereca_pics"

#boolean flag for a bad http request
flag = False;

header_html = "Content-type:text/html\r\n\r\n"
header_json = "Content-Type:application/json\r\n\n"
header_plain = "Content-type:text/plain\r\n\r\n"
#Still pretty messy

#get action from url
qs = urlparse.parse_qs(os.environ['QUERY_STRING'])
action = qs.get('action')[0]

def getUserId(username):
	cursor.execute("SELECT userId FROM userTable WHERE username = %s", username)
	user_id = str(cursor.fetchone()[0])
	return user_id

def jsonifyNote(note_info):
	note_dict = {}
	note_dict['noteId'] = note_info[0]
	note_dict['userId'] = note_info[1]
	note_dict['noteText'] = note_info[2]
	image_path = note_info[3]
	if image_path:
		file = open(image_path, "rb")
		image_bytes = file.read()
		image_string = image_bytes.encode('base64')
	else:
		image_string = "no image found"

	note_dict['image'] = image_string
	note_dict['lat'] = note_info[4]
	note_dict['lon'] = note_info[5]
	datetime = note_info[6]
	datetime_string = str(datetime)
	note_dict['date'] = datetime_string

	note_json = json.dumps(note_dict)
	return note_json


#addNote flow
if action == "addNote":
	print header_html
	#Create instance of FieldStorage 
	try:
		form = cgi.FieldStorage()
		data = form.value
		decoded_data = urllib.unquote(data).decode('utf8')
		json_data = json.loads(decoded_data)
		flag = True;
	except:
		print "POST data formatted incorrectly."


	if flag == True:
		#get Note info from JSON
		username = json_data['user']
		noteText = str(json_data['noteText']).decode('utf8')
		geo_lat = str(json_data['lat'])
		image_path = ""
		geo_lon = str(json_data['lon'])
		timestamp = time.strftime('%Y-%m-%d %H:%M:%S')
		#time_stamp = time.strptime(str(json_data['date']), '%Y-%m-%d %H:%M:%S')
		#date_stamp = time_stamp.strftime('%Y-%m-%d %H:%M:%S')
		if json_data['image']:
			image_path = pic_dir+"/"+username+"/"+str(json_data['date'])+".png"
			image_data = json_data['image']
			if not os.path.exists(pic_dir+"/"+username):
				os.makedirs(pic_dir+"/"+username)
				
			image_file = open(image_path,"wb")
			image_file.write(image_data.decode('base64'))
			image_file.close()

		#Get userId from userTable
		user_id = getUserId(username)
		#Generate note UUID
		noteId = str(uuid.uuid1())

		#Put note into database
		cursor.execute("INSERT INTO noteTable(noteId, userId, text, filepath, lat, lon, timestamp) VALUES(%s, %s, %s, %s, %s, %s, %s)", [noteId, user_id, noteText, image_path, geo_lat, geo_lon, timestamp])
		#Commit the changes
		connection.commit()
		print action + " successful"
	else:
		print action + " failed"

#getNote workflow
elif action == "getNote":
	try:
		username = qs.get('user')[0]
	except:
		print "Problems with user query parameter"
		end
		

	user_id = getUserId(username)
	
	#check for a query specifying a note
	if (qs.get('noteId')):
		note_Id = qs.get('noteId')	
		
		cursor.execute("SELECT * FROM noteTable WHERE noteId = %s ", note_Id)
	else:
		cursor.execute("SELECT * FROM noteTable WHERE userId = %s ", user_id)

	note_list = cursor.fetchmany( note_limit )

	result = jsonifyNote( note_list[0] )

	print header_html
	print result
	
#getNote workflow

#Get all the note ids of a user in a list and order by timestamp
elif action == "getNoteIDs":
	try:
		username = qs.get('user')[0]
	except:
		print "Problems with user query parameter"
		end

	user_id = getUserId(username)

	cursor.execute("SELECT noteId FROM noteTable WHERE userId = %s ORDER BY timestamp DESC", user_id)

	note_list = cursor.fetchmany(note_limit)

	#result = jsonifyNote(note_list[0])
	list = []
	for item in note_list:
		list.append( item[0] )
	result = json.dumps( list )

	print header_html
	print result
	
#addUser workflow
elif action == "addUser":
	print header_html
        #Create instance of FieldStorage 
        try:
                form = cgi.FieldStorage()
                data = form.value
                decoded_data = urllib.unquote(data).decode('utf8')
                json_data = json.loads(decoded_data)
                flag = True;
        except:
                print "POST data formatted incorrectly."


        if flag == True:
                #get user info from JSON
                username = json_data['user']
                password = json_data['password']
                #Generate user UUID
                userId = str(uuid.uuid1())

                #Put user into database
                cursor.execute("INSERT INTO userTable(userId, username, password) VALUES(%s, %s, %s)", [userId, username, password])
                #Commit the changes
                connection.commit()
                print action + " successful"
        else:
                print action + " failed"

#checkUser workflow
elif action == "checkUser":
        print header_html
        try:
                username = qs.get('user')[0]
		password = qs.get('password')[0]
        except:
                print "400: Problems with user query parameter"
		print str(qs)
                end
        cursor.execute("SELECT username, password FROM userTable WHERE username = %s", username)
        usr_pass = cursor.fetchone()
	db_username = str(usr_pass[0])
	db_password = str(usr_pass[1])
        if len(db_username) == 0:
                print "404: User not found"
		print str(db_username)
                end

        if db_password != password:
                print "401: Wrong password"
                end
        else:
            print "200: OK"

else:
        print header_html
        print "Action not recognised... Sorry bro."

