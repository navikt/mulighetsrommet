alter table nav_ansatt drop constraint nav_ansatt_pkey cascade;
alter table nav_ansatt drop constraint nav_ansatt_azure_id_key cascade;
alter table nav_ansatt add PRIMARY KEY (nav_ident, fra_ad_gruppe);
