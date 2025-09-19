update gjennomforing
set estimert_ventetid_enhet = upper(estimert_ventetid_enhet)
where estimert_ventetid_enhet is not null;
