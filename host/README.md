Embiggen Host
==============


Host runs two "servers":

1. UDP broadcast on port 8378 that sends out plain (TCP socket) host details every X seconds.

2. TCP socket for communication with controllers on random port 8379-8399.

TODO


Protocol
--------

BROADCAST UDP:
EMBIGGEN_HOST~1.2.3.4:1234

TCP:
DISPLAY_MEDIA~<url>



Notes
------
Check logs to see what IP address and port TCP socket server runs on. 
Then you should be able to browse to that host:port and get "EMBIGGEN SERVER ACK" as a response (yeah, even in a browser).