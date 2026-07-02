#let brev-layout = (
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
    "1": (size: 12pt, leading: 0pt, tracking: 0.225pt, before: 0pt),
    "2": (size: 9.75pt, leading: 0pt, tracking: 0.1875pt, before: 19.5pt),
    "3": (size: 9pt, leading: 0pt, tracking: 0.15pt, before: 19.5pt),
    "4": (size: 8.25pt, leading: 0pt, tracking: 0.075pt, before: 19.6pt),
  ),
)

#let bodyStyle = (
  size: 8.25pt,
  leading: 9pt,
  spacing: 12pt,
  weight: "regular",
)

#let footerStyle = (
  size: 6.75pt,
  leading: 0pt,
  weight: "regular",
)


#let heading-style(size, leading, tracking, space-before, body) = {
  v(space-before)
  set par(leading: leading)
  text(size: size, weight: "bold", tracking: tracking, body)
}

#let bruk-brev-stiler(body) = {
  set page(paper: "a4", margin: (top: 48pt, bottom: 55.5pt, x: 48pt), footer: context [
    #set text(size: 8pt)
    #h(1fr)
    #counter(page).display((side, total) => [side #side av #total], both: true)
  ])
  set text(lang: "no", font: "Source Sans 3", size: bodyStyle.at("size"), weight: bodyStyle.at("weight"))
  set par(justify: false, leading: bodyStyle.at("leading"), spacing: bodyStyle.at("spacing"))

  show heading.where(level: 1): it => {
    let h = headingStyle.at("levels").at("1")
    heading-style(h.at("size"), h.at("leading"), h.at("tracking"), h.at("before"), it.body)
  }

  show heading.where(level: 2): it => {
    let h = headingStyle.at("levels").at("2")
    heading-style(h.at("size"), h.at("leading"), h.at("tracking"), h.at("before"), it.body)
  }

  show heading.where(level: 3): it => {
    let h = headingStyle.at("levels").at("3")
    heading-style(h.at("size"), h.at("leading"), h.at("tracking"), h.at("before"), it.body)
  }

  show heading.where(level: 4): it => {
    let h = headingStyle.at("levels").at("4")
    heading-style(h.at("size"), h.at("leading"), h.at("tracking"), h.at("before"), it.body)
  }

  body
}

#let apply-footer-style(body) = {
  set text(size: footerStyle.at("size"), weight: footerStyle.at("weight"))
  set par(leading: footerStyle.at("leading"))
  body
}

#let info-grid(..cells) = align(left)[
  #grid(
    columns: (auto, 1fr),
    gutter: brev-layout.at("topSectionGutter"),
    ..cells,
  )
]
