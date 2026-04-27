update tiltakstype
set tiltakskode = case arena_kode
                      when 'INDJOBSTOT' then 'INDIVIDUELL_JOBBSTOTTE'
                      when 'IPSUNG' then 'INDIVIDUELL_JOBBSTOTTE_UNG'
                      when 'UTVAOONAV' then 'ARBEID_MED_STOTTE'
                      when 'ARBTREN' then 'ARBEIDSTRENING'
                      when 'MIDLONTIL' then 'MIDLERTIDIG_LONNSTLSKUDD'
                      when 'VARLONTIL' then 'VARIG_LONNSTILSKUD'
                      when 'MENTOR' then 'MENTOR'
                      when 'INKLUTILS' then 'INKLUDERINGSTILSKUD'
                      when 'TILSJOBB' then 'SOMMERJOBB'
                      when 'VATIAROR' then 'VTAO'
    end
where arena_kode in (
                     'INDJOBSTOT', 'IPSUNG', 'UTVAOONAV', 'ARBTREN',
                     'MIDLONTIL', 'VARLONTIL', 'MENTOR', 'INKLUTILS',
                     'TILSJOBB', 'VATIAROR'
    );

alter table tiltakstype
    alter tiltakskode set not null;
