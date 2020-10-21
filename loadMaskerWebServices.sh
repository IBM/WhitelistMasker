#! /bin/bash
cp ./maskerwebservices.tar.gz ./maskersvcs.tar.gz
gunzip maskersvcs.tar.gz 
docker load -i maskersvcs.tar 
docker run -v /store/WAAData/maskersvcs/properties:/opt/ol/wlp/output/defaultServer/properties --publish 9080:9080 --detach --name maskersvcs maskerwebservices
sleep 10
docker logs maskersvcs
