-- Migrerer alle delutbetalinger til status OVERFORT_TIL_UTBETALING
-- når alle delutbetalinger med samme utbetalingsId har status godkjent
UPDATE delutbetaling
SET status = 'OVERFORT_TIL_UTBETALING'
WHERE utbetaling_id IN (SELECT utbetaling_id
                        FROM delutbetaling
                        GROUP BY utbetaling_id
                        HAVING COUNT(*) = COUNT(CASE WHEN status = 'GODKJENT' THEN 1 END))
  AND status = 'GODKJENT';
