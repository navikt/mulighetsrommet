update kafka_consumer_record
set retries      = 0,
    last_retry   = null,
    headers_json = (
        '[{ "key": "kcrp-scheduled-at", "value": "MjAyNS0xMi0wNlQyMzowMDowMFo=" }]'::jsonb || headers_json::jsonb
        )::text
where retries >= 8;
