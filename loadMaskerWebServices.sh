#! /bin/bash
cp ./maskerwebservices.tar.gz ./maskersvcs.tar.gz
gunzip maskersvcs.tar.gz 
docker load -i maskersvcs.tar 
if [ ! -d "/store/WAALogs" ] 
then
   mkdir /store/WAALogs
fi
if [ ! -d "/store/WAAData/maskersvcs/properties" ] 
then
   docker run  --detach --name maskersvcs -p 9080:9080 -p 9443:9443 maskerwebservices
else
   docker run -v /store/WAALogs:logs -v /store/WAAData/maskersvcs/properties:/opt/ol/wlp/output/defaultServer/properties --detach --name maskersvcs -p 9080:9080 -p 9443:9443 maskerwebservices
fi
sleep 10
docker logs maskersvcs
