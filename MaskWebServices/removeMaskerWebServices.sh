#! /bin/bash
docker container ls
docker container stop maskersvcs
docker container rm -f maskersvcs
docker image rm -f maskerwebservices
rm -f maskersvcs.tar
rm -f maskerwebservices.tar
rm -f maskerwebservices.tar.gz
