
#import "templates/partials/styles.typ": bruk-brev-stiler
#import "templates/vedtak/vedtakinnvilgelse.typ": render-innvilgelse
#import "templates/vedtak/vedtakavslag.typ": render-avslag
#import "templates/vedtak/innsynsrett.typ": render-innsynsrett
#import "templates/vedtak/personopplysninger.typ": render-personopplysninger
#import "templates/vedtak/klagerett.typ": render-klagerett
#import "templates/vedtak/avsender.typ": signatur
#import "templates/partials/top-section.typ": render-top-section
#import "templates/partials/helpers.typ": iso-til-lang-dato

#let data = json("/data.json")

#let deltaker  = data.at("deltaker", default: none)
#let navn     = deltaker.at("fornavn", default: "") + " " + deltaker.at("etternavn", default: "")
#let fornavn   = deltaker.at("fornavn", default: "")
#let mellomn   = deltaker.at("mellomnavn", default: "")
#let etternavn = deltaker.at("etternavn", default: "")
#let fullt-navn = if mellomn != "" and mellomn != none {
  fornavn + " " + mellomn + " " + etternavn
} else {
  fornavn + " " + etternavn
  }
#let personident = deltaker.at("personident", default: "")
#let saksnummer  = data.at("saksnummer", default: "")
#let opprettet   = data.at("opprettetDato", default: "")
#let vedtak = data.at("vedtak", default: none)

#set document(
  title: "Vedtak om tilskudd til opplæring – " + navn + " (" + saksnummer + ")",
  author: "Nav",
  date: auto,
)

#bruk-brev-stiler[
  #align(left)[
    #image("/resources/logo.svg", alt: "Nav logo")
    #v(36pt)
]
#render-top-section((
  [Navn:], grid.cell(colspan: 2)[#par(fullt-navn)],
  [Fødselsnummer:], grid.cell(colspan: 2)[#par(str(personident))],
  [Saksnummer:], [#par(str(saksnummer))], align(right)[#par(str(iso-til-lang-dato(opprettet)))]
))
  #heading("Vedtak om tilskudd til opplæring", level: 1)

  #for v in vedtak {
    let utfall = v.at("utfall", default: "")

    if utfall == "Innvilgelse" { render-innvilgelse(v) }
    else  { render-avslag(v) }
  }

  #render-innsynsrett()
  #render-personopplysninger()
  #render-klagerett()
  #signatur(data)
]
