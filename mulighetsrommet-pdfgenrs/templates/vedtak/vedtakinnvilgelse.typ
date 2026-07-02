#import "templates/partials/helpers.typ": iso-til-nor-dato

#let render-innvilgelse(v) = {
  let tilskudd  = v.at("tilskuddType", default: "")
  let belop     = v.at("tilskuddBelop", default: "")
  let valuta    = v.at("valuta", default: "NOK")
  let periode   = v.at("periode", default: (:))
  let fradato   = iso-til-nor-dato(periode.at("fradato", default: ""))
  let tildato   = iso-til-nor-dato(periode.at("tildato", default: ""))
  let kommentar = v.at("kommentar", default: none)

  [
    #par[Ditt krav om #tilskudd er innvilget for perioden #fradato -- #tildato.]
    #v(0.6em)
    #par[Beløp til utbetaling: #belop #valuta]
    #v(0.6em)
    #par[Vi utbetaler til kontonummeret du har registrert hos Nav. Du kan bare registrere ett
    kontonummer hos oss. Du kan se, endre og registrere kontonummeret ditt på nav.no. Hvis du
    ikke har et kontonummer må du ta kontakt med oss.]
    #if kommentar != none and kommentar != "" [
      #v(0.6em)
      #par[#kommentar]
    ]
    #v(0.6em)
    #par[Vedtaket er fattet med hjemmel i forskrift om arbeidsmarkedstiltak
    (tiltaksforskriften) § 7-5, jf. lov om arbeidsmarkedstjenester
    (arbeidsmarkedsloven) § 13.]
  ]
}

