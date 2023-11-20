import { faro } from "@grafana/faro-web-sdk";
import { Alert, BodyShort, Button, HelpText } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useState } from "react";
import { Link } from "react-router-dom";
import { tiltaksgjennomforingTilAvtaleFilterAtom } from "../../api/atoms";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useAdminTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";
import { useAdminTiltaksgjennomforingerForKoblingTilAvtale } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforingerForKoblingTilAvtale";
import { useSetAvtaleForGjennomforing } from "../../api/tiltaksgjennomforing/useSetAvtaleForGjennomforing";
import { isTiltakMedAvtaleFraMulighetsrommet } from "../../utils/tiltakskoder";
import { Laster } from "../laster/Laster";
import { TiltaksgjennomforingstatusTag } from "../statuselementer/TiltaksgjennomforingstatusTag";
import styles from "./Tiltaksgjennomforingsliste.module.scss";

export const Tiltaksgjennomforingsliste = () => {
  const {
    data,
    isLoading,
    isError,
    refetch: refetchAvtaler,
  } = useAdminTiltaksgjennomforingerForKoblingTilAvtale();
  const { refetch: refetchTiltaksgjennomforinger } = useAdminTiltaksgjennomforinger();
  const { mutate, isPending: isPendingKobleGjennomforingForAvtale } =
    useSetAvtaleForGjennomforing();
  const { data: avtale } = useAvtale();
  const [error, setError] = useState("");

  const [filter] = useAtom(tiltaksgjennomforingTilAvtaleFilterAtom);
  const tiltaksgjennomforinger = data?.data ?? [];

  if (
    filter.search !== "" &&
    (!tiltaksgjennomforinger || tiltaksgjennomforinger.length === 0) &&
    isLoading
  ) {
    return <Laster size="xlarge" tekst="Laster tiltaksgjennomføringer..." />;
  }

  if (filter.search && tiltaksgjennomforinger.length === 0 && !isLoading) {
    return <Alert variant="info">Søk på tiltaksnummer for å finne tiltaksgjennomføringer</Alert>;
  }

  if (isError) {
    return <Alert variant="error">Vi hadde problemer med henting av tiltaksgjennomføringer</Alert>;
  }

  const handleLeggTil = (tiltaksgjennomforing: Tiltaksgjennomforing, avtaleId?: string) => {
    mutate(
      {
        gjennomforingId: tiltaksgjennomforing.id,
        avtaleId,
      },
      {
        onSettled: async () => {
          await refetchAvtaler();
          await refetchTiltaksgjennomforinger();
          faro?.api?.pushEvent(
            `Bruker ${
              avtaleId ? "kobler gjennomføring til avtale" : "fjerner gjennomføring fra avtale"
            }`,
            {
              handling: avtaleId ? "kobler til" : "fjerner kobling",
            },
            "avtale",
          );
        },
        onError: () => {
          setError(`Klarte ikke koble gjennomføring til avtale`);
        },
      },
    );
  };

  return (
    <>
      {filter.search && tiltaksgjennomforinger.length > 0 && (
        <div className={styles.gjennomforingsliste_container}>
          {error ? <Alert variant="error">{error}</Alert> : null}
          <div className={styles.gjennomforingsliste_headers}>
            <BodyShort>Tittel</BodyShort>
            <BodyShort>Tiltaksnr.</BodyShort>
            <BodyShort>Status</BodyShort>
          </div>

          <ul className={styles.gjennomforingsliste}>
            {tiltaksgjennomforinger
              .filter((gjennomforing) =>
                isTiltakMedAvtaleFraMulighetsrommet(gjennomforing.tiltakstype.arenaKode),
              )
              .map((gjennomforing: Tiltaksgjennomforing, index: number) => (
                <li key={index} className={styles.gjennomforingsliste_element}>
                  <BodyShort>{gjennomforing.navn}</BodyShort>
                  <BodyShort>{gjennomforing.tiltaksnummer}</BodyShort>
                  <TiltaksgjennomforingstatusTag tiltaksgjennomforing={gjennomforing} />
                  {!gjennomforing.avtaleId ? (
                    <Button
                      variant="tertiary"
                      className={styles.legg_til_knapp}
                      onClick={() => handleLeggTil(gjennomforing, avtale?.id)}
                      disabled={isPendingKobleGjennomforingForAvtale}
                      size="small"
                    >
                      Legg til
                    </Button>
                  ) : gjennomforing.avtaleId === avtale?.id ? (
                    <Button
                      variant="tertiary"
                      className={styles.legg_til_knapp}
                      onClick={() => handleLeggTil(gjennomforing, undefined)}
                      disabled={isPendingKobleGjennomforingForAvtale}
                      size="small"
                    >
                      Fjern
                    </Button>
                  ) : (
                    <div
                      style={{
                        margin: "0 auto",
                      }}
                    >
                      <HelpText title="Hvorfor har du ikke legg til eller fjern-knapp?">
                        Denne tiltaksgjennomføringen er allerede koblet til en annen avtale.
                        <div>
                          <Link to={`/avtaler/${gjennomforing.avtaleId}`}>Gå til avtalen</Link>
                        </div>
                      </HelpText>
                    </div>
                  )}
                </li>
              ))}
          </ul>
        </div>
      )}
    </>
  );
};
