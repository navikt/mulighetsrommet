#import "./styles.typ": letter-layout
#import "helpers.typ": iso-til-nor-dato

#let top-section(top) = {
  if top == none { return }

  grid(
    columns: (auto, 1fr, auto),
    column-gutter: letter-layout.at("topSectionColumnGutter"),
    gutter: letter-layout.at("topSectionGutter"),
    ..top
    )
    v(letter-layout.at("topSectionBottomSpacing"))

}

