import { TimerPauseFillIcon } from "@navikt/aksel-icons";
import { BodyShort } from "@navikt/ds-react";
import { EstimertVentetid as EstimertVentetidType, EstimertVentetidEnhet } from "@mr/api-client-v2";

interface Props {
  estimertVentetid: EstimertVentetidType;
}

export function EstimertVentetid({ estimertVentetid }: Props) {
  return (
    <BodyShort className="flex items-center gap-2">
      <TimerPauseFillIcon
        className="text-orange-300"
        aria-label="Stoppeklokkeikon for å indikere estimert ventetid for tiltaket"
      />
      Estimert ventetid for tiltaket:{" "}
      {formatertVentetid(estimertVentetid.verdi, estimertVentetid.enhet)}
    </BodyShort>
  );
}

function formatertVentetid(verdi: number, enhet: EstimertVentetidEnhet): string {
  switch (enhet) {
    case EstimertVentetidEnhet.UKE:
      return `${verdi} ${verdi === 1 ? "uke" : "uker"}`;
    case EstimertVentetidEnhet.MANED:
      return `${verdi} ${verdi === 1 ? "måned" : "måneder"}`;
    default:
      return "Ukjent enhet for ventetid";
  }
}
