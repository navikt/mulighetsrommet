import { VeilederflateTiltakstype } from "@api-client";
import { Button } from "@navikt/ds-react";
import { ReactNode } from "react";
import { TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL } from "@/constants";

const TEAM_TILTAK_OPPRETT_AVTALE_URL = `${TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL}/opprett-avtale`;

interface Props {
  tiltakstype: VeilederflateTiltakstype;
  harRettPaaTiltak: boolean;
}

export function OpprettAvtale({ tiltakstype, harRettPaaTiltak }: Props): ReactNode {
  if (!kanOppretteAvtaleOmTiltaksplass(tiltakstype)) {
    return null;
  }

  return (
    <Button
      variant="primary"
      data-testid="opprettavtaleknapp"
      disabled={!harRettPaaTiltak}
      onClick={() => {
        window.open(TEAM_TILTAK_OPPRETT_AVTALE_URL, "_blank");
      }}
    >
      Opprett avtale
    </Button>
  );
}

function kanOppretteAvtaleOmTiltaksplass(tiltakstype: VeilederflateTiltakstype): boolean {
  return !!tiltakstype.arenakode && whiteListOpprettAvtaleKnapp.includes(tiltakstype.arenakode);
}

// TODO: styredata på tiltakstype i stedet for å utlede om avtale kan opprettes basert på arenakoder
const whiteListOpprettAvtaleKnapp: string[] = [
  "MIDLONTIL",
  "ARBTREN",
  "VARLONTIL",
  "MENTOR",
  "INKLUTILS",
  "TILSJOBB",
  "VATIAROR",
];
