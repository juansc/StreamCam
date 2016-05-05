# StreamCam
![StreamCam Logo](https://writelatex.s3.amazonaws.com/wkfqwdphmgmd/uploads/126/5547828/1.png)
The following project was created as a Senior Project for Spring 2016 at Loyola Marymount University. 

StreamCam is an Android app that allows users to record videos and stream them to a remote server in real time. The videos are never stored on the device itself. Users can manage their videos through a web client where they can download or delete the videos that they've recorded.

StreamCam can be used to safely gather video evidence of an event. In the case of a car accident where the phone is destroyed, all the video captured up to the moment of the accident is preserved. If a user films an incident of police brutality, the officers cannot destroy the evidence by simply confiscating or destroying the phone.

## User Experience
Users interact with StreamCam using either a mobile client (Android) or a web client.

The Android client is simple. There are three screens: a login screen, a create account screen, and a camera screen. When recording, users have the option to enable the feature to track user's location. When the user hits the record button, the app begins to stream video to the Wowza Media Server. Users can terminate the video stream by tapping the record button again.

![Login Screen](https://writelatex.s3.amazonaws.com/wkfqwdphmgmd/uploads/136/5547849/2.png)
![Camera Screen](https://writelatex.s3.amazonaws.com/wkfqwdphmgmd/uploads/192/5548674/1.png)

The web client allows users to download and delete their videos. Note that users CANNOT delete videos from the Android app. They must do so using a web browser.

## Technologies Used

The following technologies were used for this project:
* Java and Android
* Node.js
* PostgreSQL
* [libstreaming](https://github.com/fyhertz/libstreaming)
* jQuery and Ajax for Web Client
* Wowza Streaming Engine

## Future Work

Given more time, we would like to implement the following features:
* Allow users to download text file that includes the users location for a given video
* Allow users to control video quality of streamed video
* Change from Wowza to open-source streaming software (possibly ffmpeg)

