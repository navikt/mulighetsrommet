import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";
import { VeilederflateTiltakstype, Tiltaksadministrasjon } from "@arbeidsmarkedstiltak/api-client";
import { Button } from "@navikt/ds-react";
import { ReactNode } from "react";
import { kanOppretteEnkeltplass } from "@/apps/modia/features";
import { useAktiveDeltakelser } from "@/api/queries/useDeltakelse";
import { InfoMeldingDeltakelse } from "./InfoMeldingDeltakelse";

interface Props {
  tiltakstype: VeilederflateTiltakstype;
  harRettPaaTiltak: boolean;
}

export function StartRegistreringEnkeltplass({ tiltakstype, harRettPaaTiltak }: Props): ReactNode {
  if (!kanOppretteEnkeltplass(tiltakstype)) {
    return null;
  }

  const { data: deltakelser } = useAktiveDeltakelser();

  const opprettDeltakelseRoute = resolveModiaRoute({
    route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE_OPPRETT_ENKELTPLASS,
    tiltakskode: tiltakstype.tiltakskode,
  });

  const pameldingAlerts =
    deltakelser &&
    deltakelser
      .filter((d): d is Tiltaksadministrasjon => d.type === "TILTAKSADMINISTRASJON")
      .map((deltakelse) => <InfoMeldingDeltakelse key={deltakelse.id} deltakelse={deltakelse} />);
  return (
    <>
      {pameldingAlerts}
      <Button
        variant="primary"
        disabled={!harRettPaaTiltak}
        onClick={opprettDeltakelseRoute.navigate}
      >
        Start registrering
      </Button>
    </>
  );
}
