-- TILTAKSTYPER
INSERT INTO public.tiltakstype (id, navn, created_at, updated_at,
                                sanity_id, tiltakskode)
VALUES ('634817ca-849c-46cd-9c6d-051a6e586eca', 'Tiltakstype 1', '2022-05-19 14:22:54.155226',
        '2022-08-04 13:37:45.382136', null, 'ABIST');
INSERT INTO public.tiltakstype (id, navn, created_at, updated_at,
                                sanity_id, tiltakskode)
VALUES ('01c43141-a99b-4cd6-bb4d-89e06e8a0006', 'Tiltakstype 2', '2022-05-19 14:22:53.603055',
        '2022-08-04 13:37:43.893975', null, 'ABOPPF');
INSERT INTO public.tiltakstype (id, navn, created_at, updated_at,
                                sanity_id, tiltakskode)
VALUES ('78457173-bc7e-4113-bafc-cdf295d7e7f5', 'Tiltakstype 3', '2022-05-19 14:22:52.881132',
        '2022-08-04 13:37:43.471029', null, 'ABUOPPF');
INSERT INTO public.tiltakstype (id, navn, created_at, updated_at,
                                sanity_id, tiltakskode)
VALUES ('4bda9407-3a50-4fd4-b534-0f521db065cf', 'Tiltakstype 4', '2022-05-19 14:22:53.664768',
        '2022-08-04 13:37:43.979626', null, 'AMBF1');
INSERT INTO public.tiltakstype (id, navn, created_at, updated_at,
                                sanity_id, tiltakskode)
VALUES ('a095bbda-6f72-451c-9ca9-2760c0111c2f', 'Tiltakstype 4', '2022-05-19 14:22:53.784776',
        '2022-08-04 13:37:44.181316', null, 'AMBF2');
INSERT INTO public.tiltakstype (id, navn, created_at, updated_at,
                                sanity_id, tiltakskode)
VALUES ('a355522c-6849-4274-93c4-ed8d6df7ff5b', 'Tiltakstype 5', '2022-05-19 14:22:53.836169',
        '2022-08-04 13:37:44.289915', null, 'AMBF3');
INSERT INTO public.tiltakstype (id, navn, created_at, updated_at,
                                sanity_id, tiltakskode)
VALUES ('8f182f5f-c847-4408-a624-51680babc07c', 'Tiltakstype 6', '2022-05-19 14:22:53.543614',
        '2022-08-04 13:37:43.785037', null, 'AMO');
INSERT INTO public.tiltakstype (id, navn, created_at, updated_at,
                                sanity_id, tiltakskode)
VALUES ('f2cc4af5-f9ee-439f-91ab-c70f72079bf4', 'Tiltakstype 7', '2022-05-19 14:22:53.726842',
        '2022-08-04 13:37:44.092230', null, 'AMOB');
INSERT INTO public.tiltakstype (id, navn, created_at, updated_at,
                                sanity_id, tiltakskode)
VALUES ('a13231fc-02ea-4b0b-90ff-c53e9b01b4c1', 'Tiltakstype 8', '2022-05-19 14:22:53.887873',
        '2022-08-04 13:37:44.928550', null, 'AMOE');
INSERT INTO public.tiltakstype (id, navn, created_at, updated_at,
                                sanity_id, tiltakskode)
VALUES ('c8a1a209-03c6-4e69-b0c8-401df45d0409', 'Tiltakstype 9', '2022-05-19 14:22:54.058364',
        '2022-08-04 13:37:45.269005', null, 'AMOY');
INSERT INTO public.tiltakstype (id, navn, created_at, updated_at,
                                sanity_id, tiltakskode)
VALUES ('e6932f3f-e501-4927-b355-6ce70a926bb3', 'Tiltakstype 10', '2022-05-19 14:22:54.058364',
        '2022-08-04 13:37:45.269005', null, 'ANNUTDANN');

-- TILTAKSGJENNOMFØRINGER
insert into public.tiltaksgjennomforing (id, navn, created_at, updated_at, sanity_id, tiltakstype_id, tiltaksnummer, virksomhetsnummer)
values ('afb69ca8-ddff-45be-9fd0-8f968519468d', 'Tiltaksgjennomføring 1', '2017-09-01 00:00:00.000000',
        '2021-06-30 00:00:00.000000', null, 'c8a1a209-03c6-4e69-b0c8-401df45d0409', '1111', '1000');
insert into public.tiltaksgjennomforing (id, navn, created_at, updated_at, sanity_id, tiltakstype_id, tiltaksnummer, virksomhetsnummer)
values ('a7d63fb0-4366-412c-84b7-7c15518ee36c', 'Tiltaksgjennomføring 2', '2017-09-01 00:00:00.000000',
        '2021-06-30 00:00:00.000000', null, 'c8a1a209-03c6-4e69-b0c8-401df45d0409', '1112', '1000');
insert into public.tiltaksgjennomforing (id, navn, created_at, updated_at, sanity_id, tiltakstype_id, tiltaksnummer, virksomhetsnummer)
values ('186df85f-c773-4f34-8904-1983787a3caf', 'Tiltaksgjennomføring 3', '2017-09-01 00:00:00.000000',
        '2021-06-30 00:00:00.000000', null, 'e6932f3f-e501-4927-b355-6ce70a926bb3', '1121', '1000');
insert into public.tiltaksgjennomforing (id, navn, created_at, updated_at, sanity_id, tiltakstype_id, tiltaksnummer, virksomhetsnummer)
values ('f9b46e0d-674c-42bd-89f6-a3180df0541c', 'Tiltaksgjennomføring 4', '2017-09-01 00:00:00.000000',
        '2021-06-30 00:00:00.000000', null, 'e6932f3f-e501-4927-b355-6ce70a926bb3', '1122', '1000');
insert into public.tiltaksgjennomforing (id, navn, created_at, updated_at, sanity_id, tiltakstype_id, tiltaksnummer, virksomhetsnummer)
values ('1e0c8f23-8ab2-4faf-bd9a-d2be60b60980', 'Tiltaksgjennomføring 5', '2017-09-01 00:00:00.000000',
        '2021-06-30 00:00:00.000000', null, '634817ca-849c-46cd-9c6d-051a6e586eca', '1211', '1000');
insert into public.tiltaksgjennomforing (id, navn, created_at, updated_at, sanity_id, tiltakstype_id, tiltaksnummer, virksomhetsnummer)
values ('20454cc4-fa45-45ae-b1eb-74c88007cdd4', 'Tiltaksgjennomføring 6', '2017-09-01 00:00:00.000000',
        '2021-06-30 00:00:00.000000', null, '634817ca-849c-46cd-9c6d-051a6e586eca', '1221', '1000');
insert into public.tiltaksgjennomforing (id, navn, created_at, updated_at, sanity_id, tiltakstype_id, tiltaksnummer, virksomhetsnummer)
values ('ea14a874-7ebd-478e-82e1-1c045c29463f', 'Tiltaksgjennomføring 7', '2017-09-01 00:00:00.000000',
        '2021-06-30 00:00:00.000000', null, '01c43141-a99b-4cd6-bb4d-89e06e8a0006', '1222', '1000');
insert into public.tiltaksgjennomforing (id, navn, created_at, updated_at, sanity_id, tiltakstype_id, tiltaksnummer, virksomhetsnummer)
values ('35ea42d5-b0cf-4de3-b249-8dbc7c02c7bb', 'Tiltaksgjennomføring 8', '2017-09-01 00:00:00.000000',
        '2021-06-30 00:00:00.000000', null, '01c43141-a99b-4cd6-bb4d-89e06e8a0006', '2111', '1000');
insert into public.tiltaksgjennomforing (id, navn, created_at, updated_at, sanity_id, tiltakstype_id, tiltaksnummer, virksomhetsnummer)
values ('d4b523a8-e6ed-4106-9090-a7c8537bfb63', 'Tiltaksgjennomføring 9', '2017-09-01 00:00:00.000000',
        '2021-06-30 00:00:00.000000', null, '78457173-bc7e-4113-bafc-cdf295d7e7f5', '2112', '1000');
insert into public.tiltaksgjennomforing (id, navn, created_at, updated_at, sanity_id, tiltakstype_id, tiltaksnummer, virksomhetsnummer)
values ('9b9225ac-4edb-4af2-8811-7448679f82e7', 'Tiltaksgjennomføring 10', '2017-09-01 00:00:00.000000',
        '2021-06-30 00:00:00.000000', null, '78457173-bc7e-4113-bafc-cdf295d7e7f5', '2121', '1000');
insert into public.tiltaksgjennomforing (id, navn, created_at, updated_at, sanity_id, tiltakstype_id, tiltaksnummer, virksomhetsnummer)
values ('3e07fa5f-3fad-456b-967c-9a72d8d30e5a', 'Tiltaksgjennomføring 11', '2017-09-01 00:00:00.000000',
        '2021-06-30 00:00:00.000000', null, '78457173-bc7e-4113-bafc-cdf295d7e7f5', '2122', '1000');
insert into public.tiltaksgjennomforing (id, navn, created_at, updated_at, sanity_id, tiltakstype_id, tiltaksnummer, virksomhetsnummer)
values ('9b6f86d7-0544-4ae3-a8fa-24c0c8e6923a', 'Tiltaksgjennomføring 12', '2017-09-01 00:00:00.000000',
        '2021-06-30 00:00:00.000000', null, '78457173-bc7e-4113-bafc-cdf295d7e7f5', '2222', '1000');
