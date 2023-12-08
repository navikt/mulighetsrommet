import { Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Toggles, VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useNavigate } from "react-router-dom";
import { useFeatureToggle } from "../../core/api/feature-toggles";
import { faneAtom } from "../../core/atoms/atoms";
import { kebabCase } from "../../utils/Utils";
import { Oppskriftsoversikt } from "../oppskrift/Oppskriftsoversikt";
import DetaljerFane from "./DetaljerFane";
import styles from "./TiltaksdetaljerFane.module.scss";
import KontaktinfoFane from "./kontaktinfofane/KontaktinfoFane";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../../utils/ErrorFallback";
import { useLogEvent } from "../../logging/amplitude";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

const TiltaksdetaljerFane = ({ tiltaksgjennomforing }: Props) => {
  const [fane, setFane] = useAtom(faneAtom);
  const navigate = useNavigate();
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

  function navigateAwayFromOppskrift() {
    navigate("./");
  }

  return (
    <Tabs
      defaultValue={fane}
      size="small"
      selectionFollowsFocus
      className={styles.fane_root}
      onChange={(value) => {
        setFane(value);
        if (value !== "tab5") {
          navigateAwayFromOppskrift();
        }
        logEvent({
          name: "arbeidsmarkedstiltak.fanevalg",
          data: {
            faneValgt: value,
          },
        });
      }}
    >
      <Tabs.List className={styles.fane_liste} id="fane_liste">
        {faneoverskrifter.filter(Boolean).map((fane, index) => (
          <Tabs.Tab
            key={index}
            value={fane}
            label={fane}
            className={styles.btn_tab}
            data-testid={`fane_${kebabCase(fane)}`}
          />
        ))}
      </Tabs.List>
      <div className={styles.fane_panel}>
        <ErrorBoundary FallbackComponent={ErrorFallback}>
          <Tabs.Panel value="For hvem" data-testid="tab1">
            <DetaljerFane
              tiltaksgjennomforingAlert={faneinnhold?.forHvemInfoboks}
              tiltakstypeAlert={tiltakstype.faneinnhold?.forHvemInfoboks}
              tiltaksgjennomforing={faneinnhold?.forHvem}
              tiltakstype={tiltakstype.faneinnhold?.forHvem}
            />
          </Tabs.Panel>
          <Tabs.Panel value="Detaljer og innhold" data-testid="tab2">
            <DetaljerFane
              tiltaksgjennomforingAlert={faneinnhold?.detaljerOgInnholdInfoboks}
              tiltakstypeAlert={tiltakstype.faneinnhold?.detaljerOgInnholdInfoboks}
              tiltaksgjennomforing={faneinnhold?.detaljerOgInnhold}
              tiltakstype={tiltakstype.faneinnhold?.detaljerOgInnhold}
            />
          </Tabs.Panel>
          <Tabs.Panel value="Påmelding og varighet" data-testid="tab3">
            <DetaljerFane
              tiltaksgjennomforingAlert={faneinnhold?.pameldingOgVarighetInfoboks}
              tiltakstypeAlert={tiltakstype.faneinnhold?.pameldingOgVarighetInfoboks}
              tiltaksgjennomforing={faneinnhold?.pameldingOgVarighet}
              tiltakstype={tiltakstype.faneinnhold?.pameldingOgVarighet}
            />
          </Tabs.Panel>
          <Tabs.Panel value="Kontaktinfo" data-testid="tab4">
            <KontaktinfoFane tiltaksgjennomforing={tiltaksgjennomforing} />
          </Tabs.Panel>
          <Tabs.Panel value="Oppskrifter" data-testid="tab5">
            <Oppskriftsoversikt tiltakstypeId={tiltaksgjennomforing.tiltakstype.sanityId} />
          </Tabs.Panel>
        </ErrorBoundary>
      </div>
    </Tabs>
  );
};

export default TiltaksdetaljerFane;
