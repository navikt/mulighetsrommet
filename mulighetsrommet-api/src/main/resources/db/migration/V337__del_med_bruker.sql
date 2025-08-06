alter table del_med_bruker
    drop updated_at,
    drop created_by,
    drop updated_by,
    drop tiltakstype_navn;

alter table del_med_bruker
    rename navident to nav_ident;

alter table del_med_bruker
    rename dialogid to dialog_id;
