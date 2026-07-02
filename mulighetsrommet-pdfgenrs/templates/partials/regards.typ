#import "styles.typ": signatureStyle
#let signatur(data) = {
  if data == none { return }

  let intro = str(data.at("intro", default: "Med vennlig hilsen"))
  let saksbehandler = str(data.at("saksbehandler", default: data.at("subject", default: "")))
  let beslutter = str(data.at("beslutter", default: ""))
  let navenhet = str(data.at("navenhet", default: ""))

  v(signatureStyle.at("spacingBefore"))
  block[
    #par(intro)
    #if saksbehandler != "" and beslutter != "" {
      grid(
        columns: signatureStyle.at("namesColumns"),
        gutter: signatureStyle.at("namesGutter"),
        [#par(saksbehandler)], [#par(beslutter)],
      )
    } else if saksbehandler != "" {
      par(saksbehandler)
    } else if beslutter != "" {
      par(beslutter)
    }

    #if navenhet != "" {
      par(navenhet)
    }
  ]
  v(signatureStyle.at("spacingAfter"))
}


#let render-hilsen(hilsen) = {
  v(1.2em)
  par(text("Med vennlig hilsen"))
  grid(
    columns: signatureStyle.at("namesColumns"),
    gutter: signatureStyle.at("namesGutter"),
  par(text(hilsen.at("subject", default: ""))),
  for annen in hilsen.at("others", default: ()) {
    par(text(annen))
  },)
}

