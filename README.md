# WhitelistMasker
This repository provides REST Service and command line utility Eclipse Maven projects for masking text content based on words identified in a whitelist.  The whitelist is built  using seed word files  and then reduced by removing names, geo-locations, and profainties before adding  back override words. When words are masked, the reason is determined by checking the word not found in the whitelist against names, geo-locations, and profanitites to provide a semantic mask like:
```
~name~ Word is not in the whitelist but is found in the names.json file
~geo~ Word is not in the whitelist but is found in the geolocations.json file
~num~ Word comprises all numeric digits (ASCII 0-9)
~url~ URL is masked because its domain matches a Domain Prefix or Suffix, or contains a Query String pattern
~misc~ Word is not found in the whitelist nor any of the names.json, geolocations.json, and is not comprised of all numeric digits (ASCII 0-9)
```

## Project Properties
Each project contains a  properties directory where content used to configure the masking services is stored. Optionally, regular expression templates may be specified to identify patterns to compare against text to replace matching content with a corresponding mask. These are defined in  the properties/maskTemplates.json file. 

## Building Projects
Each project can be  built by using the  command line: **mvn clean install** command in the project directory to write jar or war files to the target subdirectory. Alternativiely, right clicking the pom.xml file in Eclipse, selecting Run As... Maven build... and specifying  **clean  install** as the goals will build the project in Eclipse. 

### JDK Version
Content has been build using the Open  JDK version 1.8.0_242_b08 available  for download from https://adoptopenjdk.net/

### Eclipse Version
Projects  were developed in Eclipse 2020-03 available from  https://www.eclipse.org/downloads/ installing the Java EE Profile during installation.

## Masker Project
The Masker project provides the **Masker** class that enables a command  line interface to read a directory of JSON formatted dialog files to  mask their content. There is also a **MakeWhitelist** class to update the whitelist-words.json file used for  masking. The latter reads content from the properties directory and writes the updated content to the same properties directory. The jar file build in this project is used by the MaskWebServices project. 

There are also two classes to test  the MaskWebServices able to be run  from the command line or launched in Eclipse:
* **TestWSdoMasking:** reads content from the properties/Unmasked.txt file and sends a request to the MaskWebServices (as configured in the properties/MaskWebServices.properties file)
* **TestWSudpateMasks:** sends a request to the MaskWebServices to alter the regular expression mask templates

## MaskWebServices Project
The MaskWebService provides content  to generate a war file able  to be deployed to a Liberty Server by copying the target/MaskWebServices-1.0.0.war file to the Liberty server's dropins  directory. One would  also need to copy the contents of the properties directories of both the Masker and MaskWebService projects into a properties directory  in the Liberty server  directory.

## License
The  code  in this repository is licensed under the  Apache 2.0 License

## Support
It is best to open an issue in this repository. You may also contact Nathaniel Mills at wnm3@us.ibm.com.