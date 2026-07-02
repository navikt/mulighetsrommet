#import "block.typ": render-block

#let render-section(seksjon) = {
  let tittel = seksjon.at("title", default: none)
  let blocks = seksjon.at("blocks", default: ())

  if tittel != none {
    let level = tittel.at("level", default: 2)
    let tekst = tittel.at("text", default: "")
    if level == 1      { heading(level: 1)[#tekst] }
    else if level == 2 { heading(level: 2)[#tekst] }
    else if level == 3 { heading(level: 3)[#tekst] }
    else               { heading(level: 4)[#tekst] }
  }

  for block in blocks {
    render-block(block)
    v(0.4em)
  }
}

