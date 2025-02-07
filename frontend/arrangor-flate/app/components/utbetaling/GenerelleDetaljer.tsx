import { ArrFlateUtbetaling } from "@mr/api-client-v2";
import { Definisjonsliste } from "../Definisjonsliste";

interface Props {
  utbetaling: ArrFlateUtbetaling;
  className?: string;
}

export function GenerelleDetaljer({ className, utbetaling }: Props) {
  const { gjennomforing, tiltakstype } = utbetaling;

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
