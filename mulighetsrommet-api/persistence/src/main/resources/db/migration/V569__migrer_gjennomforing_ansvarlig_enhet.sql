update gjennomforing
set ansvarlig_enhet = case ansvarlig_enhet
    when '0610' then '0600'
    when '0920' then '1000'
    when '1125' then '1200'
    when '1261' then '1200'
    when '1270' then '1200'
    when '1986' then '1900'
end
where ansvarlig_enhet in ('0610', '0920', '1125', '1261', '1270', '1986');
