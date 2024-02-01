#!/bin/bash
DB_HOST=srv3-amain-o
DB_PORT=5434
DB_USER=dbuser
DB_NAME=qfsm_db
export PGPASSWORD=dbpassword

psql -U $DB_USER -d $DB_NAME -h $PACOHOST -p $PACOPORT -f ./create_config_tables.sql
psql -U $DB_USER -d $PACODB -h $PACOHOST -p $PACOPORT -f ./insert_test_data.sql
