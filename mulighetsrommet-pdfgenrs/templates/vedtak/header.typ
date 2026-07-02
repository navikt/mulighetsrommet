#import "templates/partials/helpers.typ": iso-til-lang-dato

#let render-header(data) = {
  let deltaker  = data.at("deltaker", default: (:))
  let fornavn   = deltaker.at("fornavn", default: "")
  let mellomn   = deltaker.at("mellomnavn", default: "")
  let etternavn = deltaker.at("etternavn", default: "")
  let fullt-navn = if mellomn != "" and mellomn != none {
    fornavn + " " + mellomn + " " + etternavn
  } else {
    fornavn + " " + etternavn
  }
  let personident = deltaker.at("personident", default: "")
  let saksnummer  = data.at("saksnummer", default: "")
  let opprettet   = data.at("opprettetDato", default: "")

  align(left)[
     #image("/resources/logo.svg", alt: "Nav logo")
     #v(36pt)
  ]

  grid(
    columns: (1fr, auto),
    column-gutter: 2em,
    block(above: 3em)[
      #table(
        columns: (auto, 1fr),
        stroke: none,
        inset: (x: 0pt, y: 3pt),
        table.cell(strong("Navn:")),           table.cell[#fullt-navn],
        table.cell(strong("Fødselsnummer:")),  table.cell[#personident],
        table.cell(strong("Saksnummer:")),     table.cell[#saksnummer],
      )
    ],
    block(above: 4em)[
      #align(right)[#iso-til-lang-dato(opprettet)]
    ],
  )
}

