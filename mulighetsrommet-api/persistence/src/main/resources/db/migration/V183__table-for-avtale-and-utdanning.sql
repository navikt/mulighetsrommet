create table utdanning_programomrade_avtale
(
    avtale_id uuid references avtale(id),
    programomrade_id uuid references utdanning_programomrade(id),
    utdanning_id uuid references utdanning(id)
)
