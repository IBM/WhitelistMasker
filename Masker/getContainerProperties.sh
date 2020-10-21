#! /bin/bash
if [ ! -d "/store/WAAData" ] 
then
    echo "making the /store/WAAData directory"
    mkdir /store/WAAData 
fi

if [ ! -d "/store/WAAData/maskersvcs" ] 
then
    echo "making the /store/WAAData/maskersvcs directory"
    mkdir /store/WAAData/maskersvcs 
fi

docker cp maskersvcs:/opt/ol/wlp/output/defaultServer/properties/ /store/WAAData/maskersvcs/.