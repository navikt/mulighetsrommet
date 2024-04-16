import { ArrangorKontaktpersonAnsvar } from "mulighetsrommet-api-client";

export function navnForAnsvar(
  ansvar: ArrangorKontaktpersonAnsvar,
): "Avtale" | "Tiltaksgjennomføring" | "Økonomi" {
  switch (ansvar) {
    case ArrangorKontaktpersonAnsvar.AVTALE:
      return "Avtale";
    case ArrangorKontaktpersonAnsvar.TILTAKSGJENNOMFORING:
      return "Tiltaksgjennomføring";
    case ArrangorKontaktpersonAnsvar.OKONOMI:
      return "Økonomi";
  }
}
