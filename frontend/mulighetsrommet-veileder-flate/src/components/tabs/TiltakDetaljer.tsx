import { Tabs } from "@navikt/ds-react";
import { VeilederflateTiltak } from "@mr/api-client-v2";
import { Oppskriftsoversikt } from "../oppskrift/Oppskriftsoversikt";
import { TiltakDetaljerFane } from "./TiltakDetaljerFane";
import styles from "./TiltakDetaljer.module.scss";
import { KontaktinfoFane } from "./kontaktinfofane/KontaktinfoFane";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "@/utils/ErrorFallback";
import { useLogEvent } from "@/logging/amplitude";
import { RedaksjoneltInnhold } from "../RedaksjoneltInnhold";

interface Props {
  tiltak: VeilederflateTiltak;
  setOppskriftId: (id: string | undefined) => void;
}

type TabsType = "tab1" | "tab2" | "tab3" | "tab4" | "tab5";

export function TiltakDetaljer({ tiltak, setOppskriftId }: Props) {
  const { logEvent } = useLogEvent();

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

  function getFaneValgt(value: TabsType): (typeof faneoverskrifter)[number] {
    switch (value) {
      case "tab1":
        return "For hvem";
      case "tab2":
        return "Detaljer og innhold";
      case "tab3":
        return "Påmelding og varighet";
      case "tab4":
        return "Kontaktinfo";
      case "tab5":
        return "Oppskrifter";
    }
  }

  const { tiltakstype, faneinnhold } = tiltak;

  return (
    <Tabs
      defaultValue="tab1"
      size="small"
      selectionFollowsFocus
      className={`row-start-3 max-w-none xl:row-start-[auto] xl:max-w-[auto] mb-8`}
      onChange={(value) => {
        if (value !== "tab5") {
          setOppskriftId(undefined);
        }
        logEvent({
          name: "arbeidsmarkedstiltak.fanevalg",
          data: {
            faneValgt: getFaneValgt(value as TabsType),
            tiltakstype: tiltakstype.navn,
          },
        });
      }}
    >
      <Tabs.List
        className={`${styles.fane_liste} flex flex-row border-b border-border-subtle gap-1.5 justify-start xl:justify-between`}
        id="fane_liste"
      >
        {faneoverskrifter.map((fane, index) => (
          <Tabs.Tab key={index} value={`tab${index + 1}`} label={fane} className="w-fit" />
        ))}
      </Tabs.List>
      <div className="mt-6" data-testid="fane_panel">
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
              <Oppskriftsoversikt
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
