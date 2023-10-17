import { Alert, Tabs } from "@navikt/ds-react";
import { TiltaksgjennomforingStatus } from "mulighetsrommet-api-client";
import { useState } from "react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useDeleteTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useDeleteTiltaksgjennomforing";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Laster } from "../../components/laster/Laster";
import SlettAvtaleGjennomforingModal from "../../components/modal/SlettAvtaleGjennomforingModal";
import skjemaStyles from "../../components/skjema/Skjema.module.scss";
import styles from "../DetaljerInfo.module.scss";
import { TiltaksgjennomforingDetaljer } from "./TiltaksgjennomforingDetaljer";
import { TiltaksgjennomforingKnapperad } from "./TiltaksgjennomforingKnapperad";
import { TiltaksgjennomforingRedInnhold } from "./TiltaksgjennomforingRedInnhold";

export function TiltaksgjennomforingInfo() {
  const {
    data: tiltaksgjennomforing,
    isError: isErrorTiltaksgjennomforing,
    isLoading: isLoadingTiltaksgjennomforing,
  } = useTiltaksgjennomforingById();

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
