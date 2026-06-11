delete
from flyway_schema_history
where version is null
  and starts_with(script, 'R__');
