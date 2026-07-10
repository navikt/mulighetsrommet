alter table user_notification
    add constraint user_notification_user_id_fk foreign key (user_id) references nav_ansatt (nav_ident) on delete cascade;
