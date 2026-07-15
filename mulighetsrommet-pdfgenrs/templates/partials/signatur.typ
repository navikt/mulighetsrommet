#import "styles.typ": signatureStyle

#let signatur(data) = {
  if data == none { return }

  let navenhet = data.at("enhet", default: "")
  let saksbehandler = data.at("saksbehandler", default: "")
  let beslutter = data.at("beslutter", default: "")

  v(signatureStyle.at("spacingBefore"))
  par("Med vennlig hilsen")

  if saksbehandler != "" and beslutter != "" {
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

    if navenhet != "" {
      par(navenhet)
    }

  v(signatureStyle.at("spacingAfter"))

}

