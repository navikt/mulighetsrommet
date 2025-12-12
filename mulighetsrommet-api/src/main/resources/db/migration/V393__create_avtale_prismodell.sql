create table avtale_prismodell (
   id uuid primary key default gen_random_uuid(),
   avtale_id uuid not null references avtale(id),
   prismodell_type prismodell not null,
   prisbetingelser text
);
