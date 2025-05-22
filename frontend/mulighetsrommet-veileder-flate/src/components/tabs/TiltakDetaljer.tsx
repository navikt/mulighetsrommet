import { Tabs } from "@navikt/ds-react";
import { VeilederflateTiltak } from "@mr/api-client-v2";
import { OppskriftOversikt } from "../oppskrift/OppskriftOversikt";
import { TiltakDetaljerFane } from "./TiltakDetaljerFane";
import styles from "./TiltakDetaljer.module.scss";
import { KontaktinfoFane } from "./kontaktinfofane/KontaktinfoFane";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "@/utils/ErrorFallback";
import { RedaksjoneltInnhold } from "../RedaksjoneltInnhold";

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
      className={styles.fane_root}
      onChange={(value) => {
        if (value !== "tab5") {
          setOppskriftId(undefined);
        }
      }}
    >
      <Tabs.List className={styles.fane_liste} id="fane_liste">
        {faneoverskrifter.map((fane, index) => (
          <Tabs.Tab key={index} value={`tab${index + 1}`} label={fane} className={styles.btn_tab} />
        ))}
      </Tabs.List>
      <div className={styles.fane_panel} data-testid="fane_panel">
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
            {tiltak?.faneinnhold?.oppskrift ? (
              <RedaksjoneltInnhold value={tiltak.faneinnhold.oppskrift} />
            ) : null}
            {oppskrifterEnabled && (
              <OppskriftOversikt
                tiltakstypeId={tiltakstype.sanityId}
                setOppskriftId={setOppskriftId}
              />
            )}
          </Tabs.Panel>
        </ErrorBoundary>
      </div>
    </Tabs>
  );
}

function isOppskrifterEnabled(tiltak: VeilederflateTiltak): boolean {
  if (tiltak.fylker.length < 1) {
    return true;
  }

  const fylkerSomIkkeVilHaOppskrifter = [
    "0800", // Vestfold og Telemark
  ];
  return !tiltak.fylker.some((fylke) => fylkerSomIkkeVilHaOppskrifter.includes(fylke));
}
