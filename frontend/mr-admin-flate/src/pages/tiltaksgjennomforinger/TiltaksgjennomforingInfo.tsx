import { Alert, Tabs } from "@navikt/ds-react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Laster } from "../../components/laster/Laster";
import styles from "../DetaljerInfo.module.scss";
import { TiltaksgjennomforingDetaljer } from "./TiltaksgjennomforingDetaljer";
import { TiltaksgjennomforingRedInnhold } from "./TiltaksgjennomforingRedInnhold";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { Toggles } from "mulighetsrommet-api-client";

export function TiltaksgjennomforingInfo() {
  const {
    data: tiltaksgjennomforing,
    isError: isErrorTiltaksgjennomforing,
    isLoading: isLoadingTiltaksgjennomforing,
  } = useTiltaksgjennomforingById();
  const { data: visFaneinnhold } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_FANEINNHOLD,
  );

  const { data: avtale, isLoading: isLoadingAvtale } = useAvtale(tiltaksgjennomforing?.avtaleId);

  if (isLoadingTiltaksgjennomforing || isLoadingAvtale) {
    return <Laster tekst="Laster informasjon om tiltaksgjennomføring..." />;
  }

  if (isErrorTiltaksgjennomforing) {
    return <Alert variant="error">Klarte ikke hente informasjon om tiltaksgjennomføring</Alert>;
  }

  if (!tiltaksgjennomforing) {
    return <Alert variant="warning">Fant ingen tiltaksgjennomføring</Alert>;
  }

  return (
    <div className={styles.info_container}>
      {visFaneinnhold ? (
        <Tabs defaultValue="detaljer">
          <Tabs.List>
            <Tabs.Tab value="detaljer" label="Detaljer" />
            <Tabs.Tab value="redaksjonelt_innhold" label="Redaksjonelt innhold" />
          </Tabs.List>
          <Tabs.Panel value="detaljer">
            <TiltaksgjennomforingDetaljer
              tiltaksgjennomforing={tiltaksgjennomforing}
              avtale={avtale}
            />
          </Tabs.Panel>
          <Tabs.Panel value="redaksjonelt_innhold">
            <TiltaksgjennomforingRedInnhold tiltaksgjennomforing={tiltaksgjennomforing} />
          </Tabs.Panel>
        </Tabs>
      ) : (
        <TiltaksgjennomforingDetaljer tiltaksgjennomforing={tiltaksgjennomforing} avtale={avtale} />
      )}
    </div>
  );
}
