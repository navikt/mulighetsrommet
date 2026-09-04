#import "styles.typ": signatureStyle

#let signatur(data) = {
  if data == none { return }

  let navenhet = data.at("enhet", default: "")
  let saksbehandler = data.at("saksbehandler", default: none)
  let beslutter = data.at("beslutter", default: none)

  let harSaksbehandler = saksbehandler != none and saksbehandler != ""
  let harBeslutter = beslutter != none and beslutter != ""

  v(signatureStyle.at("spacingBefore"))
  par("Med vennlig hilsen")

  if not harSaksbehandler and not harBeslutter {
    if navenhet != "" {
      par(navenhet)
    }
    v(signatureStyle.at("enhetSpacing"))
    par("Brevet er produsert automatisk og derfor ikke underskrevet av en saksbehandler.")
  } else {
    if harSaksbehandler and harBeslutter {
      grid(
        columns: signatureStyle.at("namesColumns"),
        gutter: signatureStyle.at("namesGutter"),
        [#par(beslutter)], [#par(saksbehandler)],
      )
    } else if harBeslutter {
      par(beslutter)
    } else if harSaksbehandler {
      par(saksbehandler)
    }

    if navenhet != "" {
      v(signatureStyle.at("enhetSpacing"))
      par(navenhet)
    }
  }

  v(signatureStyle.at("spacingAfter"))
}
