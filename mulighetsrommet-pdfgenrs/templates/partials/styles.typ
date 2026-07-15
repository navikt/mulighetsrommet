#let letter-layout = (
  topSectionGutter: 8pt,
  topSectionBottomSpacing: 19.5pt,
  topSectionColumnGutter: 30pt,
)

#let signatureStyle = (
  spacingBefore: 24pt,
  spacingAfter: 30pt,
  namesGutter: 12pt,
  namesColumns: (0.5fr, 0.5fr),
)

#let headingStyle = (
  levels: (
    "1": (size: 12pt,  before: 0pt, after: 0pt),
    "2": (size: 9.75pt, before: 19.5pt, after: 4.5pt),
    "3": (size: 9pt, before: 19.5pt, after: 4.5pt),
    "4": (size: 8.25pt, before: 19.5pt, after: 4.5pt),
  ),
)

#let bodyStyle = (
  size: 8.25pt,
  leading: 9pt,
  spacing: 12pt,
  weight: "regular",
)

#let heading-style(size, space-before, space-after, body) = {
  v(space-before)
  set par(leading: 0pt)
  text(size: size, tracking: 0pt, weight: "bold", body)
  v(space-after)
}

#let apply-letter-layout(body) = {
  set page(paper: "a4", margin: (top: 48pt, bottom: 55.5pt, x: 48pt), footer: context [
    #set text(size: 8pt)
    #h(1fr)
    #counter(page).display((side, total) => [side #side av #total], both: true)
  ])
  set text(lang: "no", font: "Source Sans 3", size: bodyStyle.at("size"), weight: bodyStyle.at("weight"))
  set par(justify: false, leading: bodyStyle.at("leading"), spacing: bodyStyle.at("spacing"))

  show heading.where(level: 1): it => {
    let h = headingStyle.at("levels").at("1")
    heading-style(h.at("size"), h.at("before"), h.at("after"), it.body)
  }

  show heading.where(level: 2): it => {
    let h = headingStyle.at("levels").at("2")
    heading-style(h.at("size"), h.at("before"), h.at("after"), it.body)
  }

  show heading.where(level: 3): it => {
    let h = headingStyle.at("levels").at("3")
    heading-style(h.at("size"), h.at("before"), h.at("after"), it.body)
  }

  show heading.where(level: 4): it => {
    let h = headingStyle.at("levels").at("4")
    heading-style(h.at("size"), h.at("before"), h.at("after"), it.body)
  }

  body
}

#let info-grid(..cells) = align(left)[
  #grid(
    columns: (auto, 1fr),
    gutter: letter-layout.at("topSectionGutter"),
    ..cells,
  )
]
