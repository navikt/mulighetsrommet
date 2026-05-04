import { Box, Tabs } from "@navikt/ds-react";
import { VeilederflateTiltak } from "@api-client";
import { OppskriftOversikt } from "../oppskrift/OppskriftOversikt";
import { TiltakDetaljerFane } from "./TiltakDetaljerFane";
import { KontaktinfoFane } from "./kontaktinfofane/KontaktinfoFane";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "@/utils/ErrorFallback";
import { isOppskrifterEnabled } from "@/apps/modia/features";
import { PortableText } from "@mr/frontend-common";

interface Props {
  tiltak: VeilederflateTiltak;
  setOppskriftId: (id: string | undefined) => void;
}

export function TiltakDetaljer({ tiltak, setOppskriftId }: Props) {
  const oppskrifterEnabled = isOppskrifterEnabled(tiltak);

  const faneoverskrifter = [
    "For hvem",
    "Detaljer og innhold",
    "Påmelding og varighet",
    "Kontaktinfo",
  ];

  if (oppskrifterEnabled) {
    faneoverskrifter.push("Oppskrifter");
  }

  const { tiltakstype, faneinnhold } = tiltak;

  return (
    <Tabs
      defaultValue="tab1"
      size="small"
      selectionFollowsFocus
      onChange={(value) => {
        if (value !== "tab5") {
          setOppskriftId(undefined);
        }
      }}
    >
      <Tabs.List id="fane_liste">
        {faneoverskrifter.map((fane, index) => (
          <Tabs.Tab key={index} value={`tab${index + 1}`} label={fane} className="w-fit" />
        ))}
      </Tabs.List>
      <Box margin="space-16" data-testid="fane_panel">
        <ErrorBoundary FallbackComponent={ErrorFallback}>
          <Tabs.Panel value="tab1">
            <TiltakDetaljerFane
              gjennomforingAlert={faneinnhold?.forHvemInfoboks}
              tiltakstypeAlert={tiltakstype.faneinnhold?.forHvemInfoboks}
              gjennomforing={faneinnhold?.forHvem}
              tiltakstype={tiltakstype.faneinnhold?.forHvem}
            />
          </Tabs.Panel>
          <Tabs.Panel value="tab2">
            <TiltakDetaljerFane
              gjennomforingAlert={faneinnhold?.detaljerOgInnholdInfoboks}
              tiltakstypeAlert={tiltakstype.faneinnhold?.detaljerOgInnholdInfoboks}
              gjennomforing={faneinnhold?.detaljerOgInnhold}
              tiltakstype={tiltakstype.faneinnhold?.detaljerOgInnhold}
            />
          </Tabs.Panel>
          <Tabs.Panel value="tab3">
            <TiltakDetaljerFane
              gjennomforingAlert={faneinnhold?.pameldingOgVarighetInfoboks}
              tiltakstypeAlert={tiltakstype.faneinnhold?.pameldingOgVarighetInfoboks}
              gjennomforing={faneinnhold?.pameldingOgVarighet}
              tiltakstype={tiltakstype.faneinnhold?.pameldingOgVarighet}
            />
          </Tabs.Panel>
          <Tabs.Panel value="tab4">
            <KontaktinfoFane tiltak={tiltak} />
          </Tabs.Panel>
          <Tabs.Panel value="tab5">
            {tiltak.faneinnhold?.oppskrift ? (
              <PortableText value={tiltak.faneinnhold.oppskrift} />
            ) : null}
            {oppskrifterEnabled && (
              <OppskriftOversikt
                tiltakskode={tiltakstype.tiltakskode}
                setOppskriftId={setOppskriftId}
              />
            )}
          </Tabs.Panel>
        </ErrorBoundary>
      </Box>
    </Tabs>
  );
}
