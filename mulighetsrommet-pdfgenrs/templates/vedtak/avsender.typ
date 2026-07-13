#import "../partials/styles.typ": signatureStyle
#let signatur(data) = {
  if data == none { return }

  let intro = data.at("intro", default: "Med vennlig hilsen")
  let saksbehandler = data.at("saksbehandler", default: data.at("subject", default: ""))
  let beslutter = data.at("beslutter", default: "")
  let navenhet = data.at("navenhet", default: "")

  v(signatureStyle.at("spacingBefore"))
  par(intro)
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
