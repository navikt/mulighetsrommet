import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";
import { VeilederflateTiltakstype } from "@api-client";
import { Button } from "@navikt/ds-react";
import { ReactNode } from "react";
import { kanOppretteEnkeltplass } from "@/apps/modia/features";

interface Props {
  tiltakstype: VeilederflateTiltakstype;
  harRettPaaTiltak: boolean;
}

export function StartPameldingEnkeltplass({ tiltakstype, harRettPaaTiltak }: Props): ReactNode {
  const { tiltakskode } = tiltakstype;

  if (!tiltakskode || !kanOppretteEnkeltplass(tiltakstype)) {
    return null;
  }

  const opprettDeltakelseRoute = resolveModiaRoute({
    route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE_OPPRETT_ENKELTPLASS,
    tiltakskode: tiltakskode,
  });

  return (
    <Button
      variant="primary"
      disabled={!harRettPaaTiltak}
      onClick={opprettDeltakelseRoute.navigate}
    >
      Start påmelding
    </Button>
  );
}
