# MaskWebService #
This is an Eclipse Maven Java war project.

### Build ###
Build the war using "mvn clean install". The resulting MaskWebServices-1.0.4.war  will be in the target directory.

### Install in Liberty Server ###
Copy the two .sh files into the Liberty server's directory and make them executable using the command:
```
chmod +x getMaskProps.sh
chmod +x getMaskWar.sh
```
Edit these files to reflect the location of your projects on your file system.

Execute these scripts to copy the war  file into the servers dropins directory, and to copy properties into a properties directory there.

### Build a Docker Container ###
  1. cd .. (so you are in the WhitelistMasker parent directory) 
  2. chmod +x BuildContainerMaskWebServices.sh
  3. ./BuildContainerMaskWebServices.sh
  4. Press enter to build the container


This results in a maskwebservices.tar.gz file. To install this in docker do the following:
  1. gunzip maskwebservices.tar.gz
  2. docker load -i maskwebservices.tar
  3. docker run --publish 9080:9080 --detach --name masker maskerwebservices


To check the logs once it is running:
  1. docker logs &lt;containerid_shown_when_started&gt;
  

To remove the image:
  1.  docker container ls
  2.  docker container rm -f <maskwebservices container id>
  3.  docker image ls
  4.  docker image rm -f <maskwebservices image id>
   
### Access Docker Container from Docker Hub ###
The public image is found at https://hub.docker.com/repository/docker/wnmills3/maskerwebservices and is identified as wnmills3/maskerwebservices:1.0.4

### Testing ###
In a browser, you can access the server's URL like:
```
localhost:9080/MaskWebServices/v1/HelloMasker
```

Note: change  the server and port to match your servers location.

Further testing is possible using the Masker projects TestWSdoMasking, TestWSupdateMasks, TestWSdoMessageMasking

Also, you can import the WhitelistMasker/MaskWebServices.postman_collection.json into Postman to test using its REST services.

