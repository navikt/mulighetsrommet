import { Alert, BodyShort, Button, HelpText } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useState } from "react";
import { Link } from "react-router-dom";
import { useSetAvtaleForGjennomforing } from "../../api/tiltaksgjennomforing/useSetAvtaleForGjennomforing";
import { isTiltakMedAvtaleFraMulighetsrommet } from "../../utils/tiltakskoder";
import { Laster } from "../laster/Laster";
import { TiltaksgjennomforingstatusTag } from "../statuselementer/TiltaksgjennomforingstatusTag";
import styles from "./Tiltaksgjennomforingsliste.module.scss";
import { useAdminTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";

interface Props {
  avtaleId: string;
  search: string;
}

export function Tiltaksgjennomforingsliste(props: Props) {
  const tiltakstyper = useTiltakstyper();
  const tiltakstypeIds = (tiltakstyper.data?.data ?? [])
    .filter((tiltakstype) => isTiltakMedAvtaleFraMulighetsrommet(tiltakstype.arenaKode))
    .map((tiltakstype) => tiltakstype.id);

  const { data, isPending, isError } = useAdminTiltaksgjennomforinger({
    search: props.search,
    antallGjennomforingerVises: 1000,
    tiltakstyper: tiltakstypeIds,
  });

  const { mutate, isPending: isPendingKobleGjennomforingForAvtale } =
    useSetAvtaleForGjennomforing();
  const [error, setError] = useState("");

  if (tiltakstyper.isPending || isPending) {
    return <Laster size="xlarge" tekst="Laster tiltaksgjennomføringer..." />;
  }

  if (tiltakstyper.isError || isError) {
    return <Alert variant="error">Vi hadde problemer med henting av tiltaksgjennomføringer</Alert>;
  }

  const { data: tiltaksgjennomforinger } = data;
  if (!props.search || tiltaksgjennomforinger.length === 0) {
    return <Alert variant="info">Søk på tiltaksnummer for å finne tiltaksgjennomføringer</Alert>;
  }

  const handleLeggTil = (tiltaksgjennomforing: Tiltaksgjennomforing, avtaleId?: string) => {
    mutate(
      {
        gjennomforingId: tiltaksgjennomforing.id,
        avtaleId,
      },
      {
        onError: () => {
          setError(`Klarte ikke koble gjennomføring til avtale`);
        },
      },
    );
  };

  return (
    <div className={styles.gjennomforingsliste_container}>
      {error ? <Alert variant="error">{error}</Alert> : null}
      <div className={styles.gjennomforingsliste_headers}>
        <BodyShort>Tittel</BodyShort>
        <BodyShort>Tiltaksnr.</BodyShort>
        <BodyShort>Status</BodyShort>
      </div>

      <ul className={styles.gjennomforingsliste}>
        {tiltaksgjennomforinger.map((gjennomforing) => (
          <li key={gjennomforing.id} className={styles.gjennomforingsliste_element}>
            <BodyShort>{gjennomforing.navn}</BodyShort>
            <BodyShort>{gjennomforing.tiltaksnummer}</BodyShort>
            <TiltaksgjennomforingstatusTag tiltaksgjennomforing={gjennomforing} />
            {!gjennomforing.avtaleId ? (
              <Button
                variant="tertiary"
                className={styles.legg_til_knapp}
                onClick={() => handleLeggTil(gjennomforing, props.avtaleId)}
                disabled={isPendingKobleGjennomforingForAvtale}
                size="small"
              >
                Legg til
              </Button>
            ) : gjennomforing.avtaleId === props.avtaleId ? (
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
  );
}
