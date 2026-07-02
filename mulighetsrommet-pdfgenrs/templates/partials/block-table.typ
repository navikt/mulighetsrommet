#import "helpers.typ": formater-verdi

#let render-tabell(block) = {
  let tbl = block.at("table", default: none)
  if tbl == none { return }

  let kolonner = tbl.at("columns", default: ())
  let rader    = tbl.at("rows", default: ())
  if kolonner.len() == 0 { return }

  table(
    columns: kolonner.len(),
    fill: (_, rad) => if rad > 0 and calc.odd(rad) { rgb("#f5f5f5") } else { none },
    stroke: 0.5pt + rgb("#dddddd"),
    inset: 8pt,

    ..kolonner.map(kol => {
      let juster = if kol.at("align", default: "LEFT") == "RIGHT" { right } else { left }
      table.cell(align: juster, strong(kol.at("title", default: "")))
    }),
    
    ..rader.map(rad => {
      let celler = rad.at("cells", default: ())
      celler.enumerate().map(((i, celle)) => {
        let kol    = kolonner.at(i, default: (:))
        let juster = if kol.at("align", default: "LEFT") == "RIGHT" { right } else { left }
        table.cell(align: juster, formater-verdi(celle))
      })
    }).flatten()
  )
}

