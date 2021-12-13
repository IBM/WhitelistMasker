#! /bin/bash
echo ""
echo ""
echo "This assumes you are in the WhitelistMasker/WhitelistMasker directory. If so press enter. Otherwise press Ctrl+C"
read
# build MaskWebServices
echo "Press enter to build MaskWebServices"
read
cp MaskWebServices/Dockerfile_MaskWebServices Dockerfile
docker build --no-cache --progress plain --tag tmp:1.1.8 .
docker run --publish 9080:9080 --detach --name masker tmp:1.1.8 >masker.container
docker logs "$(cat masker.container)"
docker container ls |grep masker
docker container ls |grep masker | awk '{ print $1 }' > masker.containerid
docker commit "$(cat masker.container)"  maskerwebservices
echo "Saving maskerwebservices.tar"
docker save maskerwebservices > maskerwebservices.tar
docker container stop  "$(cat masker.containerid)"
echo "removing container $(cat masker.containerid)"
docker container rm -f "$(cat masker.containerid)"
docker image ls |grep "maskerwebservices"
docker image ls |grep "maskerwebservices" | awk '{ print $3 }' > masker.imageid
echo "Removing masker.imageid $(cat masker.imageid)"
docker image rm -f "$(cat masker.imageid)"
docker image ls |grep "tmp"
docker image ls |grep "tmp" | awk '{ print $3 }' > tmpmasker.imageid
echo "Removing tmp using $(cat tmpmasker.imageid)"
docker image rm -f "$(cat tmpmasker.imageid)"
# echo "Press enter to continue cleanup"
# read
rm -f masker.container
rm -f masker.containerid
rm -f masker.imageid
rm -f tmpmasker.imageid
echo "gzipping maskerwebservices.tar"
gzip maskerwebservices.tar
