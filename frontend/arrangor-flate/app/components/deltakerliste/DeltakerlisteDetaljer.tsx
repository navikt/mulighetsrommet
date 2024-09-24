import { Deltakerliste } from "../../domene/domene";
import { Definisjonsliste } from "../Definisjonsliste";

interface Props {
  deltakerliste: Deltakerliste;
}

export function DeltakerlisteDetaljer({ deltakerliste }: Props) {
  const { tiltaksnavn, tiltaksnummer, avtalenavn, tiltakstype } = deltakerliste.detaljer;
  return (
    <Definisjonsliste
      title="Generelt"
      definitions={[
        { key: "Tiltaksnavn", value: tiltaksnavn },
        { key: "Tiltaksnummer", value: tiltaksnummer },
        { key: "Avtale", value: avtalenavn },
        { key: "Tiltakstype", value: tiltakstype },
      ]}
    />
  );
}
