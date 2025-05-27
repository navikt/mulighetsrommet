create unique index idx_unique_default_filter_per_type_per_user on lagret_filter (bruker_id, type)
    where is_default = true;
