#!/bin/bash
container_name=qfsm_db
export PGPASSWORD=db_password
dbuser=dbuser
dbname=qfsm_db
dbhost=localhost
dbport=5432

docker stop $container_name
docker run  --rm -it --name $container_name -p 5432:$dbport  -e "POSTGRES_USER=$dbuser" -e "POSTGRES_DB=$dbname" -e "POSTGRES_PASSWORD=$PGPASSWORD" -d postgres:13.3
sleep 20
echo $PGPASSWORD
awk -f paco.awk init_db.sql.orig > ./tmp/init.sql
sed -i 's/192.168.1.130:80/localhost:8081/g' ./tmp/init.sql
echo $PGPASSWORD
psql -U $dbuser -d $dbname -h $dbhost -p $dbport -f ./tmp/init.sql
