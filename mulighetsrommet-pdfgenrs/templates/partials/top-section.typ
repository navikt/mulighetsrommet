#import "./styles.typ": brev-layout
#import "helpers.typ": iso-til-nor-dato

#let render-top-section(top) = {
  if top == none { return }

  grid(
    columns: (auto, 1fr, auto),
    column-gutter: brev-layout.at("topSectionColumnGutter"),
    gutter: brev-layout.at("topSectionGutter"),
    ..top
    )
    v(brev-layout.at("topSectionBottomSpacing"))

}

