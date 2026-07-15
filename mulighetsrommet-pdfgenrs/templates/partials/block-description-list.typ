#import "helpers.typ": format-value
#import "./styles.typ": info-grid

#let description-list(block) = {
  let entries = block.at("entries", default: ())
  if entries.len() == 0 { return }

  info-grid(
    ..entries.map(entry => (
      [#par(entry.at("label", default: "") + ":")],
      [#par(format-value(entry))],
    )).flatten()
  )
}

