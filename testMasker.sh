#! /bin/bash
if [ -z $1 ]; then 
      echo ''
      echo 'Pass the protocol and hostname and port where masker is running (e.g., ./testMsaker.sh "https://localhost:9980")'
      echo ''
      echo 'Try again'
      echo ''
else 
   curl -k --location --request POST $1'/MaskWebServices/v1/masker/doMasking' --header 'Content-type: application/json' --header 'Authorization: Basic d2FhdXhzdmNzOldAdHMwbiE=' --data '{
      "request": {
         "tenantID":"companyB",
         "maskNumbers": true,
         "templates": [
         ],
         "unmasked": [
            "Bill has two emails: wnm3@us.ibm.com and lastrasl@us.ibm.com",
            "this has a name: Nathaniel Mills",
            "this has a number: 123",
            "456 started with a number",
            "Nat started with a name",
            "Connecticut started with a geographic reference",
            "https:\/\/ibm.webex.com\/join\/wnm3 started with a url",
            "This has an unrecognizable word (hereitis) <- there"
         ]
      }
   }' | jq .
fi