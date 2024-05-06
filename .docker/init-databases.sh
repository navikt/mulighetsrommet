#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	CREATE DATABASE "mr-arena-adapter";
	GRANT ALL PRIVILEGES ON DATABASE "mr-arena-adapter" TO valp;

	CREATE DATABASE "mr-api";
	GRANT ALL PRIVILEGES ON DATABASE "mr-api" TO valp;

	CREATE DATABASE "mr-tiltakshistorikk";
	GRANT ALL PRIVILEGES ON DATABASE "mr-tiltakshistorikk" TO valp;
EOSQL
