import { ArrangorKontaktpersonAnsvar } from "@tiltaksadministrasjon/api-client";

export function navnForAnsvar(
  ansvar: ArrangorKontaktpersonAnsvar,
): "Avtale" | "Gjennomføring" | "Økonomi" {
  switch (ansvar) {
    case ArrangorKontaktpersonAnsvar.AVTALE:
      return "Avtale";
    case ArrangorKontaktpersonAnsvar.GJENNOMFORING:
      return "Gjennomføring";
    case ArrangorKontaktpersonAnsvar.OKONOMI:
      return "Økonomi";
  }
}
