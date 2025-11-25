alter table arena_deltaker
    rename registrert_i_arena_dato to arena_reg_dato;

alter table arena_deltaker
    add arena_mod_dato timestamptz,
    add dager_per_uke  real,
    add deltidsprosent real;
