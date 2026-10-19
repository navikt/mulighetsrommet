drop view if exists view_avtale;
drop view if exists view_gjennomforing;
drop view if exists view_arrangorflate_tiltak;

update prismodell
set prisbetingelser = case aarsak
    when 'OPPLAERINGEN_ER_KOSTNADSFRI' then 'Opplæringen er kostnadsfri'
    when 'OPPLAERINGEN_ER_EGENFINANSIERT' then 'Bruker dekker opplæringen fullt ut selv'
end
where prismodell_type = 'INGEN_KOSTNADER'
  and prisbetingelser is null;
