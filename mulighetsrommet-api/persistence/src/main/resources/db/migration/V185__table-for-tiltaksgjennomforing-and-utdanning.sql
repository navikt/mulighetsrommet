create table utdanning_programomrade_tiltaksgjennomforing
(
    tiltaksgjennomforing_id uuid references tiltaksgjennomforing(id),
    programomrade_id uuid references utdanning_programomrade(id),
    utdanning_id uuid references utdanning(id)
)
