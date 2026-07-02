
#import "templates/partials/styles.typ": bruk-brev-stiler
#import "header.typ": render-header
#import "vedtakinnvilgelse.typ": render-innvilgelse
#import "vedtakavslag.typ": render-avslag
#import "innsynsrett.typ": render-innsynsrett
#import "personopplysninger.typ": render-personopplysninger
#import "klagerett.typ": render-klagerett
#import "avsender.typ": signatur

#let data = json("/data.json")

  #let d        = data.at("deltaker", default: (:))
  #let navn     = d.at("fornavn", default: "") + " " + d.at("etternavn", default: "")
  #let saksnr   = data.at("saksnummer", default: "")
  #set document(
    title: "Vedtak om tilskudd til opplæring – " + navn + " (" + saksnr + ")",
    author: "Nav",
    date: auto,
  )

#bruk-brev-stiler[
  #render-header(data)

  #for v in data.at("vedtak", default: ()) {
      let utfall   = v.at("utfall", default: "")
      let tilskudd = v.at("tilskuddType", default: "")

      heading(level: 2)[#utfall -- #tilskudd]

      if utfall == "Innvilgelse" { render-innvilgelse(v) }
      else                       { render-avslag(v) }
  }

  #render-innsynsrett()
  #render-personopplysninger()
  #render-klagerett()
  #signatur(data)
]
