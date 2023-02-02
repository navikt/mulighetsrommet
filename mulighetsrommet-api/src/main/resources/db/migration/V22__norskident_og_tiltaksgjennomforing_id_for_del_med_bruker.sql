alter table del_med_bruker
    rename column bruker_fnr to norsk_ident;

alter table del_med_bruker
    rename column tiltaksnummer to sanity_id;
