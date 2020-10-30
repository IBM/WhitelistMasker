# How to work with /logs 

When running the docker container, add the parameter:
````
-v <localdir>:/logs
````
to externalize the logging files to the localdir specified.

## Clearning logs
Use a script like this to clear these logs while containers are running (assuming the external logging directory is /store/WAALogs):

````
#! /bin/bash
cd /store
echo "Backing up WAALogs to ~/WAALogs.zip"
zip -r ~/WAALogs.zip ./WAALogs
ls -l ./WAALogs
echo "Press enter to truncate log files, or Ctrl+C to abort."
read 
cd /store/WAALogs
:> access.log
:> aidenerrors.log
:> aiden.log
:> Alfa_telstra_cpu.log
:> Alfa_telstra.log
:> messages.log
:> WAAOrchestrator.log
rm messages_*.log
ls -l
````
