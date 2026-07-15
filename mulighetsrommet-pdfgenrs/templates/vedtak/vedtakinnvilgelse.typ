#import "../partials/helpers.typ": iso-til-nor-dato
#import "../partials/styles.typ": bodyStyle

#let innvilgelse(vedtak) = {
  let tilskudd  = vedtak.at("tilskuddType", default: "")
  let belop     = vedtak.at("tilskuddBelop", default: "")
  let valuta    = vedtak.at("valuta", default: "NOK")
  let periode   = vedtak.at("periode", default: none)
  let fradato   = iso-til-nor-dato(periode.at("fradato", default: ""))
  let tildato   = iso-til-nor-dato(periode.at("tildato", default: ""))
  let kommentar = vedtak.at("kommentar", default: "")


  heading("Ditt krav om " + str(tilskudd) + " er innvilget for perioden " + str(fradato) + " - " + str(tildato) + ".", level: 2)
  par("Beløp til utbetaling: " + str(belop) + " " + str(valuta))

  v(bodyStyle.at("spacing"))

  par("Vi utbetaler til kontonummeret du har registrert hos Nav. Du kan bare registrere ett kontonummer hos oss. Du kan se, endre og registrere kontonummeret ditt på nav.no. Hvis du ikke har et kontonummer må du ta kontakt med oss.")
  
  v(bodyStyle.at("spacing"))

  par("Vedtaket er fattet med hjemmel i forskrift om arbeidsmarkedstiltak (tiltaksforskriften) § 7-5, jf. lov om arbeidsmarkedstjenester (arbeidsmarkedsloven) § 13.")

}

