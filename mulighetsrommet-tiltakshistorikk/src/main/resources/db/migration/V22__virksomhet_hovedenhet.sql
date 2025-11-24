with overordnet_totalt as (select distinct overordnet_enhet_organisasjonsnummer orgnr
                           from virksomhet
                           where overordnet_enhet_organisasjonsnummer is not null),
     overordnet_i_db as (select distinct organisasjonsnummer orgnr
                         from virksomhet
                         where organisasjonsnummer in (select orgnr from overordnet_totalt))
insert
into virksomhet (organisasjonsnummer)
select distinct overordnet_enhet_organisasjonsnummer
from virksomhet
where overordnet_enhet_organisasjonsnummer not in (select orgnr from overordnet_i_db)
