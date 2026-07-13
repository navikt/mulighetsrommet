#import "../partials/helpers.typ": iso-til-nor-dato
#import "../partials/styles.typ": bodyStyle

#let render-avslag(vedtak) = {
  let tilskudd  = vedtak.at("tilskuddType", default: "")
  let periode   = vedtak.at("periode", default: none)
  let fradato   = iso-til-nor-dato(periode.at("fradato", default: ""))
  let tildato   = iso-til-nor-dato(periode.at("tildato", default: ""))
  let kommentar = vedtak.at("kommentar", default: none)

  heading("Ditt krav om " + str(tilskudd) + " er avslått for perioden " + str(fradato) + " - " + str(tildato) + ".", level: 2)
  
  par("Begrunnelse:")
  par(str(kommentar))

  v(bodyStyle.at("spacing"))

  par("Vedtaket er fattet med hjemmel i forskrift om arbeidsmarkedstiltak (tiltaksforskriften) § 7-5, jf. lov om arbeidsmarkedstjenester (arbeidsmarkedsloven) § 13.")
}


