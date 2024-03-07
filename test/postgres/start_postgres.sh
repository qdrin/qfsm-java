#!/bin/bash
image=postgres:latest
container_name=psi_db
export PGPASSWORD=psi
dbuser=psi
dbname=psi_db
dbhost=localhost
dbport=5432

docker stop $container_name
docker run  --rm -it --name $container_name -p 5432:$dbport  -e "POSTGRES_USER=$dbuser" -e "POSTGRES_DB=$dbname" -e "POSTGRES_PASSWORD=$PGPASSWORD" -d $image
sleep 7
