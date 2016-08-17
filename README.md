# AndroidCoordinatedVideoPlayer
Coordinated video player to simultaneously start playback of a video on multiple tablets using UDP over WiFi. 

A simple app that listens on port 8941 for a UDP packet containing a unix timestamp. 
The timestamp indicates a time for video playback to begin. This is useful for coordinating
displays of multiple tablets that all need to play videos at once, i.e. in a store or on stage at
a conference. 
