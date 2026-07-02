#import "templates/partials/helpers.typ": iso-til-nor-dato

#let render-avslag(vedtak) = {
  let tilskudd  = vedtak.at("tilskuddType", default: "")
  let periode   = vedtak.at("periode", default: (:))
  let fradato   = iso-til-nor-dato(periode.at("fradato", default: ""))
  let tildato   = iso-til-nor-dato(periode.at("tildato", default: ""))
  let kommentar = vedtak.at("kommentar", default: none)

  [
    #par[Ditt krav om #tilskudd er avslått for perioden #fradato -- #tildato.]
    #v(0.6em)
    #par[Begrunnelse:]
    #par[#kommentar]
    #v(0.6em)
    #par[Vedtaket er fattet med hjemmel i forskrift om arbeidsmarkedstiltak
    (tiltaksforskriften) § 7-5, jf. lov om arbeidsmarkedstjenester
    (arbeidsmarkedsloven) § 13.]
  ]
}


