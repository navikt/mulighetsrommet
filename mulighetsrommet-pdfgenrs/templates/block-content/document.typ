#import "templates/partials/top-section.typ": top-section
#import "templates/partials/section.typ": section
#import "templates/partials/signatur.typ": signatur
#import "templates/partials/helpers.typ": iso-til-nor-dato
#import "templates/partials/styles.typ": apply-letter-layout

#let data = json("/data.json")

#set document(
  title: data.at("title", default: "Dokument"),
  author: data.at("author", default: "Nav"),
  date: auto,
)

#let top = data.at("topSection", default: none)

#apply-letter-layout[
    #if top != none and top.at("publicExemption", default: false) {
    align(center)[
      #par("Unntatt offentlighet, jf. offl. 13")
    ]
    v(12pt)
  }

  #align(left)[
    #image("/resources/logo.svg", alt: "Nav logo")
    #v(36pt)
  ]


  #if top != none {
    let deltaker  = top.at("deltaker", default: (:))
    let dato = top.at("date", default: none)
    let mottaker = top.at("addressedTo", default: none)
    let saksnummer = top.at("reference", default: none)

    let deltakerInfo = if deltaker != none {
      let deltakernavn = deltaker.at("navn", default: none)
      let fnr = deltaker.at("norskIdent", default: none)

      ([Navn:], grid.cell(colspan: 2)[#par(deltakernavn)],
      [Fødselsnummer:], grid.cell(colspan: 2)[#par(str(fnr))])
    } 

    top-section((
      [Mottaker:], grid.cell(colspan: 2)[#par[#mottaker]],
      ..deltakerInfo,
      [Saksnummer:], [#par[#str(saksnummer)]], align(right)[#par[#iso-til-nor-dato(dato)]],
    ))
  }
  
  #for seksjon in data.at("sections", default: none) {
    section(seksjon)
  }

  #let navenhet = data.at("enhet", default: none)
  #if navenhet != none {signatur(data)}
]

