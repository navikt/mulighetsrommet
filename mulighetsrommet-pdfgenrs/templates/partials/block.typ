#import "block-description-list.typ": description-list
#import "block-item-list.typ": itemlist
#import "block-table.typ": block-table
#import "block-paragraph.typ": paragraph
#import "./styles.typ": letter-layout, info-grid

#let render-block(block) = {
  let type = block.at("type", default: "")
  let desc = block.at("description", default: none)

  if desc != none { heading(desc, level: 2) }

  if type == "description-list"  { description-list(block) }
  else if type == "item-list"    { itemlist(block) }
  else if type == "table"        { block-table(block) }
  else if type == "paragraph"    { paragraph(block) }
}

