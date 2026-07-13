#import "../partials/styles.typ": bodyStyle
#let render-klagerett() = [
  #heading("Du kan klage på vedtaket", level: 2)

  #par("Hvis du mener vedtaket er feil, kan du klage innen [antall uker fylles ut av breveieren] uker fra den datoen vedtaket har kommet fram til deg. Dette følger av [sett inn lovhenvisning]. Du finner skjema og informasjon på nav.no/klage.")

  #v(bodyStyle.at("spacing"))

  #par("Nav kan veilede deg på telefon om hvordan du sender en klage. Nav-kontoret ditt kan også hjelpe deg med å skrive en klage. Kontakt oss på telefon 55 55 33 33.")

  #v(bodyStyle.at("spacing"))

  #par("Hvis du får medhold i klagen, kan du få dekket vesentlige utgifter som har vært nødvendige for å få endret vedtaket, for eksempel hjelp fra advokat. Du kan ha krav på fri rettshjelp etter rettshjelploven. Du kan få mer informasjon om denne ordningen hos advokater, statsforvalteren eller Nav.")

  #v(bodyStyle.at("spacing"))

  #par("Du kan lese om saksomkostninger i forvaltningsloven § 36.")

  #v(bodyStyle.at("spacing")) 
  
  #par("Hvis du sender klage i posten, må du signere klagen.")

  #v(bodyStyle.at("spacing"))
  
  #par("Mer informasjon om klagerettigheter finner du på nav.no/klagerettigheter.")
]
