#! /bin/bash
cp ./maskerwebservices.tar.gz ./maskersvcs.tar.gz
gunzip maskersvcs.tar.gz 
docker load -i maskersvcs.tar 
docker run --publish 9080:9080 --detach --name maskersvcs maskerwebservices
sleep 10
docker logs maskersvcs
