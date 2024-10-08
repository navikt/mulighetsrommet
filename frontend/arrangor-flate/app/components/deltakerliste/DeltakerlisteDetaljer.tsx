import { Deltakerliste } from "../../domene/domene";
import { Definisjonsliste } from "../Definisjonsliste";

interface Props {
  deltakerliste: Deltakerliste;
}

export function DeltakerlisteDetaljer({ deltakerliste }: Props) {
  const { tiltaksnavn, tiltakstype, refusjonskravperiode } = deltakerliste.detaljer;
  return (
    <Definisjonsliste
      title="Generelt"
      definitions={[
        { key: "Tiltaksnavn", value: tiltaksnavn },
        { key: "Tiltakstype", value: tiltakstype },
        { key: "Rapporteringsperiode", value: refusjonskravperiode },
      ]}
    />
  );
}
