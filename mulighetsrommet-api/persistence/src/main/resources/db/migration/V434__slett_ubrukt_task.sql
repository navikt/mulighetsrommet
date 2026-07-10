-- Migrasjon 380 la til denne tasken. Selv om den for lengst er borte fra miljøene så skaper den problemer for testene (inntil nå)
delete from scheduled_tasks where task_name = 'SlateTilPortableTextGjennomforing';
