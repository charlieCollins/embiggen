Embiggen Controller
====================


Controller uses UDP broadcast client for discovery of Embiggen host, and then, once discovered, uses TCP socket client
for communication with host at specified address:port (based on discovery).

Controller ALSO runs a local HTTP server for serving content at port 8999 (wifi ip address). 

NOTE: You can test the controller HTTP server directly, with a browser, etc. 
Watch logcat to see the URL it is providing to the host, and copy it. 
Ex: http://192.168.0.142:8999/storage/emulated/0/DCIM/Camera/IMG_20121225_114422.jpg



TODO

