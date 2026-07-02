#import "./styles.typ": brev-layout
#import "helpers.typ": iso-til-nor-dato

#let render-top-section(top) = {
  if top == none { return }

  let deltaker  = top.at("deltaker", default: (:))
  let fornavn   = deltaker.at("fornavn", default: "")
  let mellomn   = deltaker.at("mellomnavn", default: "")
  let etternavn = deltaker.at("etternavn", default: "")
  let deltakernavn = if mellomn != "" and mellomn != none {
    fornavn + " " + mellomn + " " + etternavn
  } else {
    fornavn + " " + etternavn
  }
  let dato = top.at("dato", default: none)
  let mottaker = top.at("mottaker", default: none)
  let fnr = deltaker.at("fnr", default: none)
  let saksnummer = top.at("saksnummer", default: none)

  grid(
    columns: (auto, 1fr, auto),
    column-gutter: brev-layout.at("topSectionColumnGutter"),
    gutter: brev-layout.at("topSectionGutter"),
    [Mottaker:], grid.cell(colspan: 2)[#par[#str(mottaker)]],
    [Saken gjelder:], grid.cell(colspan: 2)[#par[#str(deltakernavn)]],
    [Fødselsnummer:], grid.cell(colspan: 2)[#par[#str(fnr)]],
        [Saksnummer:], [#par[#str(saksnummer)]], align(right)[#par[#iso-til-nor-dato(dato)]],
    )
    v(brev-layout.at("topSectionBottomSpacing"))

}

