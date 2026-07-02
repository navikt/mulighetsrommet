#let render-avsnitt(block) = {
  let ord = block.at("words", default: ())
  par(
    ord.map(ord-item => {
      let tekst = ord-item.at("text", default: "")
      let fmt   = ord-item.at("format", default: none)
      if fmt == "BOLD" { strong(tekst) } else { tekst }
    }).join()
  )
}

