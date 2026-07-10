ALTER TABLE del_med_bruker ALTER COLUMN sanity_id TYPE uuid USING sanity_id::uuid;
ALTER TABLE del_med_bruker ALTER COLUMN sanity_id DROP NOT NULL;
ALTER TABLE del_med_bruker ADD COLUMN tiltaksgjennomforing_id uuid references tiltaksgjennomforing (id);
