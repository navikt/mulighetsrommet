#import "templates/partials/top-section.typ": render-top-section
#import "templates/partials/section.typ": render-section
#import "templates/partials/regards.typ": render-hilsen
#import "templates/partials/styles.typ": bruk-brev-stiler, apply-footer-style

#let data = json("/data.json")

#set document(
  title: data.at("title", default: "Dokument"),
  author: data.at("author", default: "Nav"),
  date: auto,
)

#bruk-brev-stiler[
  #if data.at("publicExemption", default: false) {
      align(center)[
        Unntatt offentlighet, jf. offl. 13
      ]
      v(12pt)
  }

  #align(left)[
    #image("/resources/logo.svg", alt: "Nav logo")
    #v(36pt)
  ]
#render-top-section(data.at("topSection", default: none))

#for seksjon in data.at("sections", default: ()) {
  render-section(seksjon)
}

#let hilsen = data.at("regards", default: none)
#if hilsen != none { render-hilsen(hilsen) }
]

