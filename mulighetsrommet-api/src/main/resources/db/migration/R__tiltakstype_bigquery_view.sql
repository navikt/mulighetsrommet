create or replace view tiltakstype_bigquery_view as
select navn
from tiltakstype;

ALTER DEFAULT PRIVILEGES IN SCHEMA PUBLIC GRANT SELECT ON TABLES TO "datastream";
GRANT SELECT ON tiltakstype_bigquery_view TO "datastream";

ALTER USER "mulighetsrommet-api" WITH REPLICATION;
ALTER USER "datastream" WITH REPLICATION;
CREATE PUBLICATION "ds_publication" FOR TABLE tiltakstype_bigquery_view;
SELECT PG_CREATE_LOGICAL_REPLICATION_SLOT('ds_replication', 'pgoutput');

