#! /bin/bash
if [ ! -d "/store/WAAExec" ] 
then
    echo "making the /store/WAAExec directory"
    mkdir /store/WAAExec 
fi
cd /store/WAAExec
docker cp maskersvcs:/opt/ol/wlp/usr/servers/defaultServer/apps/expanded/MaskWebServices-1.1.9.war/WEB-INF/lib/ ./Masker
cd Masker
ln -s ../../WAAData/maskersvcs/properties properties
ls -l
