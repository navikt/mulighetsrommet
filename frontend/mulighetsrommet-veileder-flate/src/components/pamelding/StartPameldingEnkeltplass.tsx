import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";
import { Tiltakskode, VeilederflateTiltakstype } from "@api-client";
import { Button } from "@navikt/ds-react";
import { ReactNode } from "react";

interface Props {
  tiltakstype: VeilederflateTiltakstype;
  harRettPaaTiltak: boolean;
}

export function StartPameldingEnkeltplass({ tiltakstype, harRettPaaTiltak }: Props): ReactNode {
  const { tiltakskode } = tiltakstype;

  if (!tiltakskode || !tiltakskoderOpprettEnkeltplass.includes(tiltakskode)) {
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

const tiltakskoderOpprettEnkeltplass = [
  Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
  Tiltakskode.STUDIESPESIALISERING,
  Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
  Tiltakskode.FAG_OG_YRKESOPPLAERING,
  Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
  Tiltakskode.HOYERE_UTDANNING,
];
