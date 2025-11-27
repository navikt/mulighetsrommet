alter table virksomhet
    add constraint fk_virksomhet_overordnet_enhet
        foreign key (overordnet_enhet_organisasjonsnummer) references virksomhet (organisasjonsnummer);

create index idx_virksomhet_overordnet_enhet on virksomhet (overordnet_enhet_organisasjonsnummer);
