Embiggen
=========

*Verb*
**_1. (rare, nonstandard) To enlarge or grow; to make or become bigger_**

Android app for sharing content (pics, videos) from a small screen to a larger one.

The *big* idea with Embiggen is that you share photos and videos with a group
by sending them from your phone/mobile to a larger screen device (a large tablet, or Google TV). 

Embiggen has two parts:   
1. HOST - Runs on the LARGE screen device and shows stuff.   
2. CONTROLLER - Runs on the SMALL screen and sends stuff to the host to embiggen it! 

Typically there is one host and one or more controllers (multiple controllers can send stuff to the same host).

See README.md in each sub-project for more info on the host and controllers. 


Networking notes
----------------
Embiggen uses UDP broadcasts to advertise and discover hosts. (It doesn't use DLNA, intentionally,
and it doesn't use multicast because it has been shown to have difficulties on various Android devices).
The discovery is very simple. 

Once a host and controller are aware of each other the controller tells the host what to display using HTTP. 
Both the host and controller run a simple HTTP server (see related charlieCollins/android-http-servers). 
The host HTTP server is used only for messaging (for controllers to be able to tell the host what to do), 
and the controller HTTP server is used for serving content to the host. 


Disclaimer
----------
This project is in the ALPHA stage, there is still a lot to do. 


Binaries
---------
https://docs.google.com/folder/d/0B1JMWIXokOXsWnQ1dzJ4dHBBc2s/edit
