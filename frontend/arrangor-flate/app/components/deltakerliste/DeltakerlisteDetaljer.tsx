import { formaterDato } from "~/utils";
import { Refusjonskrav } from "../../domene/domene";
import { Definisjonsliste } from "../Definisjonsliste";

interface Props {
  krav: Refusjonskrav;
}

export function DeltakerlisteDetaljer({ krav }: Props) {
  const { tiltaksnavn, tiltakstype, refusjonskravperiode } = krav.detaljer;
  const [start, end] = refusjonskravperiode.split(" - ");

  return (
    <Definisjonsliste
      title="Generelt"
      definitions={[
        { key: "Tiltaksnavn", value: tiltaksnavn },
        { key: "Tiltakstype", value: tiltakstype },
        { key: "Refusjonskravperiode", value: `${formaterDato(start)} - ${formaterDato(end)}` },
      ]}
    />
  );
}
