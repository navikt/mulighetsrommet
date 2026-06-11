drop view if exists view_nav_ansatt_dto;

alter table nav_ansatt
    rename column azure_id to entra_object_id;
