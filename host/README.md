Embiggen Host
==============

Host runs two "servers":

1. UDP broadcast on port 8378 that sends out plain (TCP socket) host details every X seconds.

2. HTTP server for communication with controllers on random port 8379-8399.
(HTTP server port is randomized so that multiple hosts can run on the same LAN segment.)


Protocol
--------

BROADCAST UDP:
EMBIGGEN_HOST~INSTALL_ID~1.2.3.4~1234

HTTP:
http://HOST_ADDR:HOST_PORT?DISPLAY_MEDIA=localUrl

FUTURE: slideshows, etc


Notes
------
Check logs to see what IP address and port the HTTP server is chosen to run on.
