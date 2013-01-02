Embiggen
=========

Android app(s) for sharing content (pics, videos) from one or more "controllers" to a "host."

The big idea with Embiggen is that you share photos and videos with a group
by sending them from your phone to a larger screen device (a big tablet, or Google TV, etc). 

Embiggen has two parts:   
1. HOST - The Host app sits around waiting for requests to show stuff, it runs on the BIG screen.   
2. CONTROLLER - The controller app runs on your phone/tablet and sends content to the host. 

See README.md in each sub-project for more info. 

Networking notes
----------------
Embiggen uses UDP broadcasts to advertise and discover hosts. (It doesn't use DLNA, intentionally,
and it doesn't use multicast because it has been shown to have difficulties on various Android devices).
The discovery is very simple. 

Once a host and controller are aware of each other the controller tells the host what to display using HTTP. 
Both the host and controller run a simple HTTP server (see sub-project). The host HTTP server is used
only for messaging (for controllers to be able to tell the host what to do), and the controller HTTP server
is used for serving content to the host. 


Disclaimer
----------
This project is in the ALPHA stage, there is still a lot to do. 
