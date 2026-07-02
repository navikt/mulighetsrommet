#import "block-description-list.typ": render-beskrivelsesliste
#import "block-item-list.typ": render-elementliste
#import "block-table.typ": render-tabell
#import "block-paragraph.typ": render-avsnitt
#import "./styles.typ": brev-layout, info-grid

#let render-block(block) = {
  let type = block.at("type", default: "")
  let desc = block.at("description", default: none)

  if desc != none { par(desc) }

  if type == "description-list"  { render-beskrivelsesliste(block) }
  else if type == "item-list"    { render-elementliste(block) }
  else if type == "table"        { render-tabell(block) }
  else if type == "paragraph"    { render-avsnitt(block) }
}

