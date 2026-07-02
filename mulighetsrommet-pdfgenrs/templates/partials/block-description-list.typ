#import "helpers.typ": formater-verdi
#import "./styles.typ": info-grid

#let render-beskrivelsesliste(block) = {
  let entries = block.at("entries", default: ())
  if entries.len() == 0 { return }

  info-grid(
    ..entries.map(entry => (
      [#par(entry.at("label", default: "") + ":")],
      [#par(formater-verdi(entry))],
    )).flatten()
  )
}

