import { Definisjonsliste } from "../Definisjonsliste";
import { RefusjonKravAft } from "@mr/api-client-v2";

interface Props {
  krav: RefusjonKravAft;
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
