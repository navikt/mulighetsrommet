import { Refusjonskrav } from "~/domene/domene";
import { Definisjonsliste } from "../Definisjonsliste";

interface Props {
  krav: Refusjonskrav;
  className?: string;
}

export function GenerelleDetaljer({ className, krav }: Props) {
  const { tiltaksnavn, tiltakstype } = krav.detaljer;

  return (
    <Definisjonsliste
      className={className}
      title="Generelt"
      definitions={[
        { key: "Tiltaksnavn", value: tiltaksnavn },
        { key: "Tiltakstype", value: tiltakstype },
      ]}
    />
  );
}
