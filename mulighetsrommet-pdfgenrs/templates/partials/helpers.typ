#let maaneder = (
  "januar", "februar", "mars", "april", "mai", "juni",
  "juli", "august", "september", "oktober", "november", "desember",
)

#let iso-til-nor-dato(s) = {
  if s == none or s == "" { return "-" }
  let dato-str = str(s).split("T").first()
  let deler = dato-str.split("-")
  if deler.len() >= 3 {
    deler.at(2) + "." + deler.at(1) + "." + deler.at(0)
  } else {
    s
  }
}

#let iso-til-lang-dato(s) = {
  if s == none or s == "" { return "" }
  let deler = str(s).split("-")
  if deler.len() >= 3 {
    str(int(deler.at(2))) + ". " + maaneder.at(int(deler.at(1)) - 1) + " " + deler.at(0)
  } else {
    s
  }
}

#let formater-verdi(entry) = {
  let verdi  = entry.at("value", default: none)
  let fmt    = entry.at("format", default: none)
  let valuta = entry.at("currency", default: none)

  if verdi == none {
    [-]
  } else if fmt == "DATE" {
    [#iso-til-nor-dato(str(verdi))]
  } else if fmt == "PERCENT" {
    [#verdi %]
  } else if fmt == "STATUS_SUCCESS" {
    box(
      fill: rgb("#ccf1d6"),
      stroke: 1pt + rgb("#06893a"),
      inset: (x: 3pt, y: 2pt),
    )[#text(size: 10pt, verdi)]
  } else if valuta != none {
    [#verdi #valuta]
  } else {
    [#verdi]
  }
}

