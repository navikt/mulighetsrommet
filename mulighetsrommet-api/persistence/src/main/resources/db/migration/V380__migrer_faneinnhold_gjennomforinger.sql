-- Slate formatet for redaksjonelt innhold var litt forskjellig fra portable text formatet
-- Tasken ble kjørt manuelt for avtaler, men gjør det samme nå for gjennomføringer
insert into scheduled_tasks (task_name,
                             task_instance,
                             execution_time,
                             picked,
                             version)
values ('SlateTilPortableTextGjennomforing',
        '41538b74-bf9b-4bb5-ba89-1417c3ba5597',
        '2025-11-24 15:00:00.000000 +00:00',
        false,
        1
        )
