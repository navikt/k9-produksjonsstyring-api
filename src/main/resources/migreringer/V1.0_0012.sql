create function sort_array(jsonb, text) returns jsonb
    language sql IMMUTABLE
as
$$
select jsonb_agg(value order by (value->$2))
from jsonb_array_elements($1)
$$;

