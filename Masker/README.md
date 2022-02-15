# Masker Project v1.1.9 #
This is  an Eclipse Maven Java project.

### JDK Version ###
Content has been build using the OpenJDK Runtime Environment (build 1.8.0_252-b09) available  for download from https://adoptopenjdk.net/

### Eclipse Version ###
Projects  were developed in Eclipse 2021-09 available from  https://www.eclipse.org/downloads/ installing the Java EE Profile during installation.

### Build ###
Build using mvn clean install

### Testing ###
If you have also built the MaskerWebServices project and installed the .war file in a Liberty server's dropins directory, and copied the properties from the MaskerWebServices and Masker into the Liberty server's directory and started the server, you can run these programs:

### TestWSdoMessageMasking ###

Right click on TestWSdoMessageMasking and select Run As... / Java Application

This showcases the newest feature to mask a conversation, and return the masked values in a diffs map. The reason for this is to allow applications wanting to mask PII before sending the content to other models in the Cloud by running the masker on their intranet. The diffs map allows the calling code to maintain the original content in memory so it can replace masked content later. 

**Below is an example of the console running TestWSdoMessageMasking:**
```
Enter the tenant ID or q to exit (companyA):

Enter the filename of the web service properties file (MaskWebService.properties) or q  to quit:

Enter the filename of the conversation to be masked (UnmaskedMessages.json) or q to quit:

Should numbers be masked, or q to quit(Y)

Enter the hostname where the service is running or q to quit (localhost)

Sending Request:
{
   "request": {
      "maskNumbers": true,
      "messages": [
         {
            "speaker": "client",
            "utterance": "Hi, I'm Nat Mills and I wanted to get a new iPhone for my daughter Lauren"
         },
         {
            "speaker": "client",
            "time": "2019-12-19T15:07:37.677-05:00",
            "utterance": "this has a ~ here ~ and two emails: wnm3@us.ibm.com and lastrasl@us.ibm.com"
         },
         {
            "speaker": "bot",
            "time": "2019-12-19T15:07:38.677-05:00",
            "utterance": "this has a name: Nathaniel Mills"
         },
         {
            "speaker": "client",
            "time": "2019-12-19T15:07:39.677-05:00",
            "utterance": "this has a number: 123"
         },
         {
            "speaker": "agent",
            "time": "2019-12-19T15:07:40.677-05:00",
            "utterance": "456 started with a number"
         },
         {
            "speaker": "client",
            "time": "2019-12-19T15:07:41.677-05:00",
            "utterance": "Bill started with a name"
         },
         {
            "speaker": "agent",
            "time": "2019-12-19T15:07:42.677-05:00",
            "utterance": "Connecticut started with a geographic reference"
         },
         {
            "speaker": "client",
            "time": "2019-12-19T15:07:43.677-05:00",
            "utterance": "https:\/\/ibm.webex.com\/join\/wnm3 started with a url"
         },
         {
            "speaker": "agent",
            "time": "2019-12-19T15:07:44.677-05:00",
            "utterance": "This has an unrecognizable word (hereitis) <- there"
         },
         {
            "speaker": "client",
            "time": "2019-12-19T15:07:45.677-05:00",
            "utterance": "Here are some US phone numbers: 8608125089, 860.812.5089, 860-8125089, 860.812-5089, +1 (860)812-5089 and another +18608125089"
         }
      ],
      "templates": [
         {
            "mask": "~Phone",
            "template": "\\+?1? *\\(?\\d{3}\\)? *[\\-\\.]?(\\d{3}) *[\\-\\.]? *\\d{4}"
         }
      ],
      "tenantID": "companyA"
   }
}
Full Response:
{
   "results": {
      "diffs": [
         {
            "~name~": "Nat Mills"
         },
         {
            "~name~": "Lauren"
         },
         {
            "~email~": "wnm3@us.ibm.com"
         },
         {
            "~email~": "lastrasl@us.ibm.com"
         },
         {
            "~name~": "Nathaniel Mills"
         },
         {
            "~num~": "123"
         },
         {
            "~num~": "456"
         },
         {
            "~name~": "Bill"
         },
         {
            "~geo~": "Connecticut"
         },
         {
            "~misc~": "hereitis"
         },
         {
            "~phone~": " 8608125089"
         },
         {
            "~phone~": " 860.812.5089"
         },
         {
            "~phone~": " 860-8125089"
         },
         {
            "~phone~": " 860.812-5089"
         },
         {
            "~phone~": "+1 (860)812-5089"
         },
         {
            "~phone~": "+18608125089"
         }
      ],
      "errors": [
      ],
      "maskedMessages": [
         {
            "speaker": "client",
            "utterance": "Hi, I'm ~name~ and I wanted to get a new iPhone for my daughter ~name~"
         },
         {
            "speaker": "client",
            "time": "2019-12-19T15:07:37.677-05:00",
            "utterance": "this has a ~ here ~ and two emails: ~email~ and ~email~"
         },
         {
            "speaker": "bot",
            "time": "2019-12-19T15:07:38.677-05:00",
            "utterance": "this has a name: ~name~"
         },
         {
            "speaker": "client",
            "time": "2019-12-19T15:07:39.677-05:00",
            "utterance": "this has a number: ~num~"
         },
         {
            "speaker": "agent",
            "time": "2019-12-19T15:07:40.677-05:00",
            "utterance": "~num~ started with a number"
         },
         {
            "speaker": "client",
            "time": "2019-12-19T15:07:41.677-05:00",
            "utterance": "~name~ started with a name"
         },
         {
            "speaker": "agent",
            "time": "2019-12-19T15:07:42.677-05:00",
            "utterance": "~geo~ started with a geographic reference"
         },
         {
            "speaker": "client",
            "time": "2019-12-19T15:07:43.677-05:00",
            "utterance": "https:\/\/ibm.webex.com\/join\/wnm3  started with a url"
         },
         {
            "speaker": "agent",
            "time": "2019-12-19T15:07:44.677-05:00",
            "utterance": "This has an unrecognizable word (~misc~) <- there"
         },
         {
            "speaker": "client",
            "time": "2019-12-19T15:07:45.677-05:00",
            "utterance": "Here are some US phone numbers:~phone~,~phone~,~phone~,~phone~, ~phone~ and another ~phone~"
         }
      ]
   }
}
Enter the tenant ID or q to exit (companyA):
q
Goodbye
```

Note: because of the way the mask for phone is specified, the leading blank is included in the masked value when no +1 exists.

### TestWSdoMasking ###
Right click on TestWSdoMasking and select Run As... / Java Application

**Below is an example of the console running TestWSdoMasking:**
```
Enter the tenant ID or q to exit (companyA):

Enter the filename of the web service properties file (MaskWebService.properties) or q  to quit:

Enter the filename of the text to be masked (Unmasked.txt) or q to quit:

Should numbers be masked, or q to quit(Y)

Enter the hostname where the service is running or q to quit (localhost)

Sending Request:
{
   "request": {
      "maskNumbers": true,
      "templates": [
         {
            "mask": "~Phone",
            "template": "\\+?1? *\\(?\\d{3}\\)? *[\\-\\.]?(\\d{3}) *[\\-\\.]? *\\d{4}"
         }
      ],
      "tenantID": "companyA",
      "unmasked": [
         "this has two emails: wnm3@us.ibm.com and lastrasl@us.ibm.com",
         "this has a name: Nathaniel Mills",
         "this has a number: 123",
         "456 started with a number",
         "Bill started with a name",
         "Connecticut started with a geographic reference",
         "https:\/\/ibm.webex.com\/join\/wnm3 started with a url",
         "This has an unrecognizable word (hereitis) <- there",
         "Here are some US phone numbers: 8608125089, 860.812.5089, 860-8125089, 860.812-5089, +1 (860)812-5089 and another +18608125089"
      ]
   }
}
Full Response:
{
   "results": {
      "errors": [
      ],
      "masked": [
         "this has two emails: ~email~ and ~email~",
         "this has a name: ~name~",
         "this has a number: ~num~",
         "~num~ started with a number",
         "~name~ started with a name",
         "~geo~ started with a geographic reference",
         "~url~ started with a url",
         "This has an unrecognizable word (~misc~) <-  there",
         "Here are some US phone numbers:~phone~,~phone~,~phone~,~phone~, ~phone~ and another ~phone~"
      ]
   }
}
Enter the tenant ID or q to exit (companyA):
companyB
Enter the filename of the web service properties file (MaskWebService.properties) or q  to quit:

Enter the filename of the text to be masked (Unmasked.txt) or q to quit:

Should numbers be masked, or q to quit(Y)

Enter the hostname where the service is running or q to quit (localhost)

Sending Request:
{
   "request": {
      "maskNumbers": true,
      "templates": [
         {
            "mask": "~Phone",
            "template": "\\+?1? *\\(?\\d{3}\\)? *[\\-\\.]?(\\d{3}) *[\\-\\.]? *\\d{4}"
         }
      ],
      "tenantID": "companyB",
      "unmasked": [
         "this has two emails: wnm3@us.ibm.com and lastrasl@us.ibm.com",
         "this has a name: Nathaniel Mills",
         "this has a number: 123",
         "456 started with a number",
         "Bill started with a name",
         "Connecticut started with a geographic reference",
         "https:\/\/ibm.webex.com\/join\/wnm3 started with a url",
         "This has an unrecognizable word (hereitis) <- there",
         "Here are some US phone numbers: 8608125089, 860.812.5089, 860-8125089, 860.812-5089, +1 (860)812-5089 and another +18608125089"
      ]
   }
}
Full Response:
{
   "results": {
      "errors": [
      ],
      "masked": [
         "this has two emails: ~email~ and ~email~",
         "this has a name: ~name~",
         "this has a number: ~num~",
         "~num~ started with a number",
         "Bill started with a name",
         "~geo~ started with a geographic reference",
         "~url~ started with a url",
         "This has an unrecognizable word (~misc~) <-  there",
         "Here are some US phone numbers:~phone~,~phone~,~phone~,~phone~, ~phone~ and another ~phone~"
      ]
   }
}
Enter the tenant ID or q to exit (companyA):
q
Goodbye
```
### TestWSupdateMasks ###
Right click on TestWSupdateMasks and select Run As... / Java Application

**Below is an example of the console running TestWSupdateMaska:**
```
Enter the tenant ID or q to exit (companyA):

Enter the fully qualified filename of the web service properties file (MaskWebService.properties) or q  to quit:

Sending Request:
{
   "request": {
      "removals": [
         "([a-zA-Z])+(\\w)*((\\.(\\w)+)?)+@([a-zA-Z])+(\\w)*((\\.([a-zA-Z])+(\\w)*)+)?(\\.)[a-zA-Z]{2,}"
      ],
      "tenantID": "companyA",
      "updates": [
         {
            "mask": "~Phone",
            "template": "\\+?1? *\\(?\\d{3}\\)? *[\\-\\.]?(\\d{3}) *[\\-\\.]? *\\d{4}"
         }
      ]
   }
}
Full Response:
{
   "results": {
      "errors": [
      ],
      "removed": [
         {
            "mask": "email",
            "template": "([a-zA-Z])+(\\w)*((\\.(\\w)+)?)+@([a-zA-Z])+(\\w)*((\\.([a-zA-Z])+(\\w)*)+)?(\\.)[a-zA-Z]{2,}"
         }
      ],
      "updated": [
         {
            "mask": "~Phone",
            "template": "\\+?1? *\\(?\\d{3}\\)? *[\\-\\.]?(\\d{3}) *[\\-\\.]? *\\d{4}"
         }
      ]
   }
}
```


Examples to externalize the container properties directory and to set up external Masker directories to allow building / updating the whitelist-words.json. These examples assume a parent directory /store to hold WAAData and WAAExec but you can edit the commands to use whatever directory you want.

### Externalize the properties directory ###
There is a properties directory installed in the container at /opt/ol/wlp/output/defaultServer/properties. You can copy this to an external directory using the docker cp command:
```
docker cp maskersvcs:/opt/ol/wlp/output/defaultServer/properties/ /store/WAAData/maskersvcs/.
```

Or you can use the **getContainerProperties.sh** in the Masker project.

There is a script called **setupMasker.sh** in the Masker project. This script will extract the classes and jar files from the container to the /store/WAAExec/Masker directory and create a symbolic link to the /store/WAAData/maskersvcs/properties directory.

Once these are done, you can edit the override-words.txt in the properties directory and then use the **runMakeWhiteListWords.sh** in the Masker project to update the whitelist-words.json file.

Finally to have this updated file read into the container, use the docker commands:
````
docker container stop maskersvcs
docker container start maskersvcs
````

Now you can take advantage of the **loadMaskerWebServices.sh** script in the WhitelistMasker directory to laod the docker comtainer mounting the externalized properties directory on your localhost into the container. Similarly, **removeMaskerWebServices.sh** will remove the container.