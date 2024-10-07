import { Refusjonskrav } from "../../domene/domene";
import { Definisjonsliste } from "../Definisjonsliste";

interface Props {
  krav: Refusjonskrav;
}

export function DeltakerlisteDetaljer({ krav }: Props) {
  const { tiltaksnavn, tiltaksnummer, avtalenavn, tiltakstype, refusjonskravperiode } =
    krav.detaljer;
  return (
    <Definisjonsliste
      title="Generelt"
      definitions={[
        { key: "Tiltaksnavn", value: tiltaksnavn },
        { key: "Tiltaksnummer", value: tiltaksnummer },
        { key: "Avtale", value: avtalenavn },
        { key: "Tiltakstype", value: tiltakstype },
        { key: "Rapporteringsperiode", value: refusjonskravperiode },
      ]}
    />
  );
}
