# Masker Project #
This is  an Eclipse Maven Java project.

### JDK Version ###
Content has been build using the Open  JDK version 1.8.0_242_b08 available  for download from https://adoptopenjdk.net/

### Eclipse Version ###
Projects  were developed in Eclipse 2020-03 available from  https://www.eclipse.org/downloads/ installing the Java EE Profile during installation.

### Build ###
Build using mvn clean install

### Testing ###
If you have also built the MaskerWebServices project and installed the .war file in a Liberty server's dropins directory, and copied the properties from the MaskerWebServices and Masker into the Liberty server's directory and started the server, you can run these programs:

#### TestWSdoMasking ####
Right click on TestWSdoMasking and select Run As... / Java Application
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
####TestWSupdateMasks ####
Right click on TestWSupdateMasks and select Run As... / Java Application
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