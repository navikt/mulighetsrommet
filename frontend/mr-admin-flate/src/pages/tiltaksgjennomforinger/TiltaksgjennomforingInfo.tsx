import { Alert, Tabs } from "@navikt/ds-react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Laster } from "../../components/laster/Laster";
import styles from "../DetaljerInfo.module.scss";
import skjemaStyles from "../../components/skjema/Skjema.module.scss";
import { TiltaksgjennomforingDetaljer } from "./TiltaksgjennomforingDetaljer";
import { TiltaksgjennomforingRedInnhold } from "./TiltaksgjennomforingRedInnhold";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { TiltaksgjennomforingStatus, Toggles } from "mulighetsrommet-api-client";
import { TiltaksgjennomforingKnapperad } from "./TiltaksgjennomforingKnapperad";
import { useDeleteTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useDeleteTiltaksgjennomforing";
import { useState } from "react";
import SlettAvtaleGjennomforingModal from "../../components/modal/SlettAvtaleGjennomforingModal";

export function TiltaksgjennomforingInfo() {
  const {
    data: tiltaksgjennomforing,
    isError: isErrorTiltaksgjennomforing,
    isLoading: isLoadingTiltaksgjennomforing,
  } = useTiltaksgjennomforingById();
  const { data: visFaneinnhold } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_FANEINNHOLD,
  );

  const [slettModal, setSlettModal] = useState(false);
  const mutation = useDeleteTiltaksgjennomforing();

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

  function visKnapperad(status: TiltaksgjennomforingStatus): boolean {
    const whitelist: TiltaksgjennomforingStatus[] = [
      TiltaksgjennomforingStatus.GJENNOMFORES,
      TiltaksgjennomforingStatus.APENT_FOR_INNSOK,
    ];

    return whitelist.includes(status);
  }

  return (
    <div className={styles.info_container}>
      {visFaneinnhold ? (
        <Tabs defaultValue="detaljer">
          <Tabs.List className={skjemaStyles.tabslist}>
            <div>
              <Tabs.Tab value="detaljer" label="Detaljer" />
              <Tabs.Tab value="redaksjonelt_innhold" label="Redaksjonelt innhold" />
            </div>
            {visKnapperad(tiltaksgjennomforing.status) && (
              <TiltaksgjennomforingKnapperad handleSlett={() => setSlettModal(true)} />
            )}
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
        <>
          <TiltaksgjennomforingDetaljer
            tiltaksgjennomforing={tiltaksgjennomforing}
            avtale={avtale}
          />
          {visKnapperad(tiltaksgjennomforing.status) && (
            <TiltaksgjennomforingKnapperad
              style={{
                display: "flex",
                flexDirection: "row",
                justifyContent: "flex-end",
              }}
              handleSlett={() => setSlettModal(true)}
            />
          )}
        </>
      )}
      <SlettAvtaleGjennomforingModal
        modalOpen={slettModal}
        handleCancel={() => setSlettModal(false)}
        data={tiltaksgjennomforing}
        mutation={mutation}
        dataType={"tiltaksgjennomforing"}
      />
    </div>
  );
}
