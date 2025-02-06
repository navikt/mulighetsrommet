import { ArrFlateRefusjonKrav } from "@mr/api-client-v2";
import { Definisjonsliste } from "../Definisjonsliste";

interface Props {
  krav: ArrFlateRefusjonKrav;
  className?: string;
}

export function GenerelleDetaljer({ className, krav }: Props) {
  const { gjennomforing, tiltakstype } = krav;

  return (
    <Definisjonsliste
      className={className}
      title="Generelt"
      definitions={[
        { key: "Tiltaksnavn", value: gjennomforing.navn },
        { key: "Tiltakstype", value: tiltakstype.navn },
      ]}
    />
  );
}
