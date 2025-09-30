# Sign Language Detection Desktop Application

## Overview
This is a desktop application built using JavaFX (Java) and Python that detects sign language gestures in real-time.  
It captures gestures through your computer's camera and translates them into the corresponding alphabet letter.

**Note**: We chose to develop this application using the English alphabet because our goal was to create a tool that can assist the widest number of users globally. English is one of the most commonly used languages worldwide, and American Sign Language (ASL) is widely recognized. Therefore, the project, including the model and interface, has been fully adjusted to support English letters and ASL hand signs.

## How It Works
- Use your right hand to perform the gesture for the letter you want to sign.
- Make sure you are using a white background for better accuracy.
- Hold the gesture steady for about five seconds.
- The model will detect the gesture and display the corresponding alphabet letter.

## Features
- Real-time sign language detection.
- JavaFX-based desktop interface.
- Python-based gesture recognition model.
- Easy to use: no setup needed, everything runs through Docker.

## Requirements
- Docker installed on your machine
- A working camera

## Installation and Running

1. Pull the Docker image:
- docker pull sohaila1745/speakurhands:v1
 
2. Run the Docker container:

- xhost +local:root
sudo docker run --rm \
  --name speakurhands-app \
  -e DISPLAY=$DISPLAY \
  -v /tmp/.X11-unix:/tmp/.X11-unix \
  -v $HOME/.Xauthority:/root/.Xauthority \
  --env XAUTHORITY=/root/.Xauthority \
  --net=host \
  --privileged \
  --device /dev/video0:/dev/video0 \
  sohaila1745/speakurhands:v1

- Docker Hub URL
You can find the image here:
https://hub.docker.com/repository/docker/sohaila1745/speakurhands/general 

_No manual setup required. All dependencies are included inside the Docker image._

## Notes
- Ensure your camera is connected and functioning properly.
- Always use a white background for better gesture recognition.
- Make sure your hand is fully visible to the camera and avoid rapid movements.
-The source code includes a speakurhands.jar file, which is the application packaged in  JAR format. This file is used to create the Docker image and run the container.
-The training folder contains the model and the files used to train it, but not the  dataset. The dataset is a collection of several small datasets that we excluded from  the source code because the ZIP file size became 1.7 GB, making it impossible to send.

## License
All rights reserved.

