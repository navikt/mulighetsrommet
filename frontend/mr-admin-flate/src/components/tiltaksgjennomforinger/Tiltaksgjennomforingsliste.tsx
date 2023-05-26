import { MinusIcon, PlusIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, HelpText } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { tiltaksgjennomforingTilAvtaleFilter } from "../../api/atoms";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useAdminTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";
import { useAdminTiltaksgjennomforingerForAvtale } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforingerForAvtale";
import { useMutateKobleGjennomforingForAvtale } from "../../api/tiltaksgjennomforing/useMutateKobleGjennomforingForAvtale";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { Laster } from "../laster/Laster";
import { Tiltaksgjennomforingstatus } from "../statuselementer/Tiltaksgjennomforingstatus";
import styles from "./Tiltaksgjennomforingsliste.module.scss";

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

  const handleLeggTil = (
    tiltaksgjennomforing: Tiltaksgjennomforing,
    avtaleId?: string
  ) => {
    mutate(
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
                ) : gjennomforing.avtaleId === avtale?.id ? (
                  <Button
                    variant="tertiary"
                    className={styles.legg_til_knapp}
                    onClick={() => handleLeggTil(gjennomforing, undefined)}
                    disabled={isLoadingKobleGjennomforingForAvtale}
                  >
                    <MinusIcon fontSize={22} />
                    Fjern
                  </Button>
                ) : (
                  <div style={{ margin: "0 auto" }}>
                    <HelpText title="Hvorfor har du ikke legg til eller fjern-knapp?">
                      Denne tiltaksgjennomføringen er allerede koblet til
                      avtalen med avtalenavn <b>{avtale?.navn}</b>.
                    </HelpText>
                  </div>
                )}
              </div>
            )
          )}
        </div>
      )}
    </>
  );
};
