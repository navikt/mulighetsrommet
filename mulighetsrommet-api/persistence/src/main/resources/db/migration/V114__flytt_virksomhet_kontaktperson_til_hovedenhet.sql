update virksomhet_kontaktperson
set organisasjonsnummer = subq.overordnet_enhet
from (
    select coalesce(overordnet_enhet, organisasjonsnummer) as overordnet_enhet, organisasjonsnummer from virksomhet
) as subq
where subq.organisasjonsnummer = virksomhet_kontaktperson.organisasjonsnummer;
