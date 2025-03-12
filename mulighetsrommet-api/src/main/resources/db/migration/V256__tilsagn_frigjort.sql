drop view if exists tilsagn_admin_dto_view;
drop view if exists tilsagn_arrangorflate_view;

alter table tilsagn add column status tilsagn_status;
UPDATE tilsagn
SET status = subquery.status::tilsagn_status
FROM (
    SELECT
        t.id AS tilsagn_id,
        tilsagn_status(
            opprettelse.besluttelse,
            annullering.behandlet_av,
            annullering.besluttelse
        ) AS status
    FROM tilsagn t
    LEFT JOIN LATERAL (
        SELECT *
        FROM totrinnskontroll
        WHERE totrinnskontroll.entity_id = t.id
          AND totrinnskontroll.type = 'OPPRETT'
        ORDER BY totrinnskontroll.behandlet_tidspunkt DESC
        LIMIT 1
    ) AS opprettelse ON true
    LEFT JOIN LATERAL (
        SELECT *
        FROM totrinnskontroll
        WHERE totrinnskontroll.entity_id = t.id
          AND totrinnskontroll.type = 'ANNULLER'
        ORDER BY totrinnskontroll.behandlet_tidspunkt DESC
        LIMIT 1
    ) AS annullering ON true
) AS subquery
WHERE tilsagn.id = subquery.tilsagn_id;

drop function if exists tilsagn_status;

alter table tilsagn alter column status set not null;

alter type totrinnskontroll_type add value 'FRIGJOR';

alter type tilsagn_status add value 'TIL_FRIGJORING';
alter type tilsagn_status add value 'FRIGJORT';
