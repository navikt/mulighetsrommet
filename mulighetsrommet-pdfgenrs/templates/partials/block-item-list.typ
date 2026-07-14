#let itemlist(block) = {
  let items = block.at("items", default: ())
  list(..items.map(item => [#item]))
}

