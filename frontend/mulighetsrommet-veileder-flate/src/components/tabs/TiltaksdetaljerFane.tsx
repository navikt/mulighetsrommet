import { Tabs } from "@navikt/ds-react";
import { Toggles, VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useFeatureToggle } from "@/api/feature-toggles";
import { Oppskriftsoversikt } from "../oppskrift/Oppskriftsoversikt";
import DetaljerFane from "./DetaljerFane";
import styles from "./TiltaksdetaljerFane.module.scss";
import KontaktinfoFane from "./kontaktinfofane/KontaktinfoFane";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../../utils/ErrorFallback";
import { useLogEvent } from "../../logging/amplitude";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
  setOppskriftId: (id: string | undefined) => void;
}

type TabsType = "tab1" | "tab2" | "tab3" | "tab4" | "tab5";

const TiltaksdetaljerFane = ({ tiltaksgjennomforing, setOppskriftId }: Props) => {
  const { logEvent } = useLogEvent();

  const { data: enableArenaOppskrifter } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_ARENA_OPPSKRIFTER,
  );

  const { tiltakstype, faneinnhold } = tiltaksgjennomforing;
  const faneoverskrifter = [
    "For hvem",
    "Detaljer og innhold",
    "Påmelding og varighet",
    "Kontaktinfo",
    enableArenaOppskrifter ? "Oppskrifter" : "",
  ] as const;

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
        logEvent({
          name: "arbeidsmarkedstiltak.fanevalg",
          data: {
            faneValgt: getFaneValgt(value as TabsType),
            tiltakstype: tiltaksgjennomforing.tiltakstype.navn,
          },
        });
      }}
    >
      <Tabs.List className={styles.fane_liste} id="fane_liste">
        {faneoverskrifter.filter(Boolean).map((fane, index) => (
          <Tabs.Tab key={index} value={`tab${index + 1}`} label={fane} className={styles.btn_tab} />
        ))}
      </Tabs.List>
      <div className={styles.fane_panel} data-testid="fane_panel">
        <ErrorBoundary FallbackComponent={ErrorFallback}>
          <Tabs.Panel value="tab1">
            <DetaljerFane
              tiltaksgjennomforingAlert={faneinnhold?.forHvemInfoboks}
              tiltakstypeAlert={tiltakstype.faneinnhold?.forHvemInfoboks}
              tiltaksgjennomforing={faneinnhold?.forHvem}
              tiltakstype={tiltakstype.faneinnhold?.forHvem}
            />
          </Tabs.Panel>
          <Tabs.Panel value="tab2">
            <DetaljerFane
              tiltaksgjennomforingAlert={faneinnhold?.detaljerOgInnholdInfoboks}
              tiltakstypeAlert={tiltakstype.faneinnhold?.detaljerOgInnholdInfoboks}
              tiltaksgjennomforing={faneinnhold?.detaljerOgInnhold}
              tiltakstype={tiltakstype.faneinnhold?.detaljerOgInnhold}
            />
          </Tabs.Panel>
          <Tabs.Panel value="tab3">
            <DetaljerFane
              tiltaksgjennomforingAlert={faneinnhold?.pameldingOgVarighetInfoboks}
              tiltakstypeAlert={tiltakstype.faneinnhold?.pameldingOgVarighetInfoboks}
              tiltaksgjennomforing={faneinnhold?.pameldingOgVarighet}
              tiltakstype={tiltakstype.faneinnhold?.pameldingOgVarighet}
            />
          </Tabs.Panel>
          <Tabs.Panel value="tab4">
            <KontaktinfoFane tiltaksgjennomforing={tiltaksgjennomforing} />
          </Tabs.Panel>
          <Tabs.Panel value="tab5">
            <Oppskriftsoversikt
              tiltakstypeId={tiltaksgjennomforing.tiltakstype.sanityId}
              setOppskriftId={setOppskriftId}
            />
          </Tabs.Panel>
        </ErrorBoundary>
      </div>
    </Tabs>
  );
};

export default TiltaksdetaljerFane;
