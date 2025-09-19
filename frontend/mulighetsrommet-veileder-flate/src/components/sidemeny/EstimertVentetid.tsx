import { TimerPauseFillIcon } from "@navikt/aksel-icons";
import { BodyShort } from "@navikt/ds-react";

interface Props {
  estimertVentetid: string;
}

export function EstimertVentetid({ estimertVentetid }: Props) {
  return (
    <BodyShort className="flex items-center gap-2">
      <TimerPauseFillIcon
        className="text-orange-300"
        aria-label="Stoppeklokkeikon for å indikere estimert ventetid for tiltaket"
      />
      Estimert ventetid for tiltaket: {estimertVentetid}
    </BodyShort>
  );
}
