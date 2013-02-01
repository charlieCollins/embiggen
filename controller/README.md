Embiggen Controller
====================

Controller uses UDP broadcast client for discovery of Embiggen host.
Once discovered, uses HTTP requests to communicate with host and tell it what media to display.

Controller ALSO runs a local HTTP server for serving content at port 8999 (wifi ip address). 

NOTE: You can test the controller HTTP server directly, with a browser, etc. 
Watch logcat to see the URL it is providing to the host, and copy it. 
Ex: http://IP_ADDR:8999/storage/emulated/0/DCIM/Camera/SOME_IMAGE.jpg

