# Send UDP broadcast packets

MYPORT = 8941

import sys, time, struct
from socket import *

s = socket(AF_INET, SOCK_DGRAM)
s.bind(('10.42.0.1', 0))
s.setsockopt(SOL_SOCKET, SO_BROADCAST, 1)
print int(time.time())
data = struct.pack("!q", int(time.time()))
s.sendto(data, ('10.42.0.255', MYPORT))
