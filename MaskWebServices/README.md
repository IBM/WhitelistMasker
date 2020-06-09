# MaskWebService #
This is an Eclipse Maven Java war project.

### Build ###
Build the war using "mvn clean install". It  will be in the target directory.

### Install in Liberty Server ###
Copy the two .sh files into the Liberty server's directory and make them executable using the command:
```
chmod +x getMaskProps.sh
chmod +x getMaskWar.sh
```
Edit these files to reflect the location of your projects on your file system.

Execute these scripts to copy the war  file into the servers dropins directory, and to copy properties into a properties directory there.

### Testing ###
In a browser, you can access the server's URL like:
```
localhost:9080/MaskWebServices/v1/HelloMasker
```
Note: change  the server and port to match your servers location.

Further testing is possible using the Masker projects TestWSdoMasking and TestWSupdateMasks
