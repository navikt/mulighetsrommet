insert into endringshistorikk(document_id,
                              value,
                              operation,
                              user_id,
                              document_class,
                              edited_at)
select du.utbetaling_id,
       '{"migrering": "V382-2025-11-25"}'::jsonb,
       concat('Betaling for tilsagn ', t.bestillingsnummer, ' er utbetalt'),
       'Tiltaksadministrasjon',
       'UTBETALING',
       du.faktura_status_sist_oppdatert
from delutbetaling du
         inner join tilsagn t on du.tilsagn_id = t.id
where du.faktura_status in ('FULLT_BETALT','DELVIS_BETALT')
