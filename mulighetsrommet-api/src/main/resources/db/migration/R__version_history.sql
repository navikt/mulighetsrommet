/**
 * Utility for å dumpe snaphots av et dokument til en tabell spesifisert i [versioning_table].
 *
 * versioning_table: tabell som skal skrives til
 * operation: fritekst som beskriver endringen
 * document_id: id til dokumentet/entiteten som blir endret
 * value: en JSON dump av dokumentet/entiteten
 * user_id: identifikator på den som gjør endringen (typisk NAVIdent).
 * ts: timestamp for endringen. Utledes automatisk, men kan settes eksplisitt.
 *     Må være etter (i tid) forrige rad med samme [document_id].
 */
create or replace function version_history(
    operation text,
    document_id uuid,
    document_class document_class,
    value jsonb,
    user_id text,
    ts timestamp with time zone default current_timestamp
) returns void as
$$
declare
    last_sys_period tstzrange;
    sql_query       text;
begin
    -- Find the last sys_period for the given document_id
    sql_query := 'SELECT sys_period FROM endringshistorikk WHERE document_id = $1 ORDER BY sys_period DESC LIMIT 1';
    execute sql_query into last_sys_period using document_id;

    -- If a previous record for "id", validate
    if last_sys_period is not null then
        if lower(last_sys_period) > ts then
            raise invalid_parameter_value using
                message =
                            'the "ts" parameter must be after the most recent "sys_period" for versions of record with document_id ' ||
                            document_id,
                hint = 'expected "ts" to be after ' || lower(last_sys_period);
        end if;

        -- Update the sys_period of the previous latest record
        sql_query := 'UPDATE endringshistorikk SET sys_period = tstzrange($1, $2, ''[)'')' ||
                     ' WHERE document_id = $3 AND sys_period = $4';
        execute sql_query using lower(last_sys_period), ts, document_id, last_sys_period;
    end if;

    -- Insert the new record with the updated sys_period
    sql_query := 'INSERT INTO endringshistorikk' ||
                 ' (operation, document_id, document_class, value, user_id, sys_period) VALUES ($1, $2, $3, $4, $5, tstzrange($6, NULL, ''[)''))';
    execute sql_query using operation, document_id, document_class, value, user_id, ts;
end
$$ language plpgsql;

