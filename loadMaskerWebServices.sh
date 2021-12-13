#! /bin/bash
docker load -i maskerwebservices.tar.gz 
if [ ! -d "/store/WAALogs/maskersvcs" ] 
then
   mkdir -p /store/WAALogs/maskersvcs
fi
if [ ! -d "/store/WAAData/maskersvcs/properties" ] 
then
   docker run  --detach --name maskersvcs -p 9080:9080 maskerwebservices
else
   docker run -v /store/WAALogs/maskersvcs:/logs -v /store/WAAData/maskersvcs/properties:/opt/ol/wlp/output/defaultServer/properties --detach --name maskersvcs -p 9080:9080 -p 9980:9980 maskerwebservices
fi
sleep 10
docker logs maskersvcs
