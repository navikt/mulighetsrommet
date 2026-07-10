drop view if exists tilsagn_admin_dto_view;
drop view if exists tilsagn_arrangorflate_view;

alter table tilsagn add column belop_gjenstaende int;

update tilsagn set belop_gjenstaende =
    (tilsagn.beregning->'output'->>'belop')::int
    - coalesce((
        select sum(d.belop) from delutbetaling d
        where d.tilsagn_id = tilsagn.id
        and d.sendt_til_okonomi_tidspunkt is not null
    ), 0);

alter table tilsagn alter column belop_gjenstaende set not null;
