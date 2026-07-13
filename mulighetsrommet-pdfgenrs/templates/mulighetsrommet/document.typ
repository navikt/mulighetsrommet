#import "templates/partials/top-section.typ": render-top-section
#import "templates/partials/section.typ": render-section
#import "templates/partials/regards.typ": render-hilsen
#import "templates/partials/helpers.typ": iso-til-nor-dato, format-name
#import "templates/partials/styles.typ": bruk-brev-stiler, apply-footer-style

#let data = json("/data.json")

#set document(
  title: data.at("title", default: "Dokument"),
  author: data.at("author", default: "Nav"),
  date: auto,
)

#let top = data.at("topSection", default: none)
#let deltaker  = top.at("deltaker", default: none)
#let fnr = deltaker.at("fnr", default: none)
#let dato = top.at("dato", default: none)
#let mottaker = top.at("mottaker", default: none)
#let saksnummer = top.at("saksnummer", default: none)


#bruk-brev-stiler[
  #if data.at("publicExemption", default: false) {
      align(center)[
        #par("Unntatt offentlighet, jf. offl. 13")
      ]
      v(12pt)
  }

  #align(left)[
    #image("/resources/logo.svg", alt: "Nav logo")
    #v(36pt)
  ]

  #let fornavn   = deltaker.at("fornavn", default: "")
  #let mellomn   = deltaker.at("mellomnavn", default: "")
  #let etternavn = deltaker.at("etternavn", default: "")
  #let deltakernavn = format-name(fornavn, mellomn, etternavn)
  #let deltakerinfo = if deltakernavn != none and fnr != none {[[Navn:], grid.cell(colspan: 2)[#par(deltakernavn)],
      [Fødselsnummer:], grid.cell(colspan: 2)[#par(str(fnr))]]}

  #render-top-section((
    [Mottaker:], grid.cell(colspan: 2)[#par[#mottaker]],
    ..deltakerinfo,
    [Saksnummer:], [#par[#str(saksnummer)]], align(right)[#par[#iso-til-nor-dato(dato)]],
  ))

  #for seksjon in data.at("sections", default: none) {
    render-section(seksjon)
  }

  #let hilsen = data.at("regards", default: none)
  #if hilsen != none { render-hilsen(hilsen) }
]

