# MaskWebService v1.2.7 #
This is an Eclipse Maven Java war project.

### JDK Version ###
Content has been build using the IBM Semeru Runtime Certified Edition 11.0.17.0 (build 11.0.17+8)  

### Eclipse Version ###
Projects  were developed in Eclipse 2022-12 available from  https://www.eclipse.org/downloads/ installing the Java EE Profile during installation.

### Build ###
Build the war using "mvn clean install -Dgpg-skip". The resulting MaskWebServices-1.2.7.war  will be in the target directory.

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
  3. docker run --publish 9080:9080 --detach --name masker -e MASK_ADMIN_USER=&lt;user&gt; -e MASK_ADMIN_PASSWORD=&lt;password&gt; maskerwebservices

Note: you can also use the -v option to load an external properties directory into the container making it easier to change the data and load it by stopping and starting the container


To check the logs once it is running:
  1. docker logs &lt;containerid_shown_when_started&gt;
  

To remove the image:
  1.  docker container ls
  2.  docker container rm -f <maskwebservices container id>
  3.  docker image ls
  4.  docker image rm -f <maskwebservices image id>
   
### Access Docker Container from Docker Hub ###
The public image is found at https://hub.docker.com/r/wnmills3/maskerwebservices and is identified as wnmills3/maskerwebservices:1.2.7

### Security ###
`POST v1/masker/updateMasks` changes the masking templates of a tenant for every later request, so it requires HTTP BASIC
authentication as a member of the `MaskAdmin` group defined in server.xml. Set the credentials of that user with the
environment variables:
  - `MASK_ADMIN_USER` (defaults to `maskadmin`)
  - `MASK_ADMIN_PASSWORD` (no default; if it is not set no user can authenticate and updateMasks is refused)

Serve the application over HTTPS (port 9980) when sending credentials. `doMasking` and `doMessageMasking` do not
require authentication.

Cross-origin browser requests are allowed from any origin without credentials. To allow credentialed requests from
specific origins, list them comma separated in the `MASK_CORS_ALLOWED_ORIGINS` environment variable.

Mask labels (the `mask` of a template) may only contain letters and digits.

Applying templates to a line is limited to one second (`Masker._templateTimeoutMillis`) so a template whose regex
backtracks catastrophically can not tie up the server. A line that runs out of time is returned as `~misc~` and an
error is reported, so it is never returned partially masked.

### Testing ###
In a browser, you can access the server's URL like:
```
localhost:9080/MaskWebServices/v1/HelloMasker
```

Note: change  the server and port to match your servers location.

Further testing is possible using the Masker projects TestWSdoMasking, TestWSupdateMasks, TestWSdoMessageMasking

Also, you can import the WhitelistMasker/MaskWebServices.postman_collection.json into Postman to test using its REST services.

