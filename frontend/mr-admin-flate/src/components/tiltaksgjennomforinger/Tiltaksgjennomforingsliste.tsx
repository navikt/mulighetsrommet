import { Alert, BodyShort, Button } from "@navikt/ds-react";
import { Tiltaksgjennomforingstatus } from "../statuselementer/Tiltaksgjennomforingstatus";
import React from "react";
import { PlusIcon } from "@navikt/aksel-icons";
import styles from "./Tiltaksgjennomforingsliste.module.scss";
import { useAdminTiltaksgjennomforingerForAvtale } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforingerForAvtale";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useAtom } from "jotai";
import { tiltaksgjennomforingTilAvtaleFilter } from "../../api/atoms";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { useAdminTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";
import { useMutateKobleGjennomforingForAvtale } from "../../api/tiltaksgjennomforing/useMutateKobleGjennomforingForAvtale";
import { Laster } from "../laster/Laster";

export const Tiltaksgjennomforingsliste = () => {
  const {
    data,
    isLoading,
    isError,
    refetch: refetchAvtaler,
  } = useAdminTiltaksgjennomforingerForAvtale();
  const { refetch: refetchTiltaksgjennomforinger } =
    useAdminTiltaksgjennomforinger();
  const { mutate, isLoading: isLoadingKobleGjennomforingForAvtale } =
    useMutateKobleGjennomforingForAvtale();
  const avtaleId = useGetAvtaleIdFromUrl();
  const { data: avtale } = useAvtale(avtaleId);

  const [filter] = useAtom(tiltaksgjennomforingTilAvtaleFilter);
  const tiltaksgjennomforinger = data?.data ?? [];

  if (
    filter.search !== "" &&
    (!tiltaksgjennomforinger || tiltaksgjennomforinger.length === 0) &&
    isLoading
  ) {
    return <Laster size="xlarge" tekst="Laster tiltaksgjennomføringer..." />;
  }

  if (filter.search && tiltaksgjennomforinger.length === 0 && !isLoading) {
    return (
      <Alert variant="info">
        Søk på tiltaksnummer for å finne tiltaksgjennomføringer
      </Alert>
    );
  }

  if (isError) {
    return (
      <Alert variant="error">
        Vi hadde problemer med henting av tiltaksgjennomføringer
      </Alert>
    );
  }

  const handleLeggTil = async (
    tiltaksgjennomforing: Tiltaksgjennomforing,
    avtaleId?: string
  ) => {
    await mutate(
      { gjennomforingId: tiltaksgjennomforing.id, avtaleId },
      {
        onSettled: async () => {
          await refetchAvtaler();
          await refetchTiltaksgjennomforinger();
        },
      }
    );
  };

  return (
    <>
      {filter.search && tiltaksgjennomforinger.length > 0 && (
        <div className={styles.gjennomforingsliste_container}>
          <div className={styles.gjennomforingsliste_headers}>
            <BodyShort>Tittel</BodyShort>
            <BodyShort>Tiltaksnr.</BodyShort>
            <BodyShort>Status</BodyShort>
          </div>

          {tiltaksgjennomforinger.map(
            (gjennomforing: Tiltaksgjennomforing, index: number) => (
              <div key={index} className={styles.gjennomforingsliste}>
                <BodyShort>{gjennomforing.navn}</BodyShort>
                <BodyShort>{gjennomforing.tiltaksnummer}</BodyShort>
                <Tiltaksgjennomforingstatus
                  tiltaksgjennomforing={gjennomforing}
                />
                {!gjennomforing.avtaleId ? (
                  <Button
                    variant="tertiary"
                    className={styles.legg_til_knapp}
                    onClick={() => handleLeggTil(gjennomforing, avtale?.id)}
                    disabled={isLoadingKobleGjennomforingForAvtale}
                  >
                    <PlusIcon fontSize={22} />
                    Legg til
                  </Button>
                ) : null}
              </div>
            )
          )}
        </div>
      )}
    </>
  );
};
