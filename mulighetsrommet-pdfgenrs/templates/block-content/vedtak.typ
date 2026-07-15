
#import "templates/partials/styles.typ": apply-letter-layout
#import "templates/vedtak/vedtakinnvilgelse.typ": innvilgelse
#import "templates/vedtak/vedtakavslag.typ": avslag
#import "templates/vedtak/innsynsrett.typ": innsynsrett
#import "templates/vedtak/personopplysninger.typ": personopplysninger
#import "templates/vedtak/klagerett.typ": klagerett
#import "templates/partials/signatur.typ": signatur
#import "templates/partials/top-section.typ": top-section
#import "templates/partials/helpers.typ": iso-til-lang-dato

#let data = json("/data.json")

#let deltaker  = data.at("deltaker", default: none)
#let navn     = deltaker.at("navn", default: "")
#let fnr = deltaker.at("norskIdent", default: "")
#let saksnummer  = data.at("saksnummer", default: "")
#let opprettet   = data.at("opprettetDato", default: "")
#let vedtak = data.at("vedtak", default: none)

#set document(
  title: "Vedtak om tilskudd til opplæring – " + navn + " (" + saksnummer + ")",
  author: "Nav",
  date: auto,
)

#apply-letter-layout[
  #align(left)[
    #image("/resources/logo.svg", alt: "Nav logo")
    #v(36pt)
]
#top-section((
  [Navn:], grid.cell(colspan: 2)[#par(navn)],
  [Fødselsnummer:], grid.cell(colspan: 2)[#par(str(fnr))],
  [Saksnummer:], [#par(str(saksnummer))], align(right)[#par(str(iso-til-lang-dato(opprettet)))]
))
  #heading("Vedtak om tilskudd til opplæring", level: 1)

  #for v in vedtak {
    let utfall = v.at("utfall", default: "")

    if utfall == "Innvilgelse" { innvilgelse(v) }
    else  { avslag(v) }
  }

  #innsynsrett()
  #personopplysninger()
  #klagerett()
  #signatur(data)
]
