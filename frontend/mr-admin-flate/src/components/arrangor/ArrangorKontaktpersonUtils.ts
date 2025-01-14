import { ArrangorKontaktpersonAnsvar } from "@mr/api-client";

export function navnForAnsvar(
  ansvar: ArrangorKontaktpersonAnsvar,
): "Avtale" | "Gjennomføring" | "Økonomi" {
  switch (ansvar) {
    case ArrangorKontaktpersonAnsvar.AVTALE:
      return "Avtale";
    case ArrangorKontaktpersonAnsvar.TILTAKSGJENNOMFORING:
      return "Gjennomføring";
    case ArrangorKontaktpersonAnsvar.OKONOMI:
      return "Økonomi";
  }
}
