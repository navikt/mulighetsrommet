import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { formaterDato, inneholderUrl } from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Laster } from "../../components/laster/Laster";
import { Alert, Button, Checkbox, Link } from "@navikt/ds-react";
import classNames from "classnames";
import { useState } from "react";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { OpprettTiltaksgjennomforingModal } from "../../components/modal/OpprettTiltaksgjennomforingModal";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { ExternalLinkIcon } from "@navikt/aksel-icons";

export function TiltaksgjennomforingInfo() {
  const {
    data: tiltaksgjennomforing,
    isError: isErrorTiltaksgjennomforing,
    isLoading: isLoadingTiltaksgjennomforing,
  } = useTiltaksgjennomforingById();
  const { data: avtale, isLoading: isLoadingAvtale } = useAvtale(
    tiltaksgjennomforing?.avtaleId
  );
  const { data: features } = useFeatureToggles();

  const [redigerModal, setRedigerModal] = useState(false);
  const handleRediger = () => setRedigerModal(true);
  const lukkRedigerModal = () => setRedigerModal(false);

  const sanityTiltaksgjennomforingUrl =
    "https://mulighetsrommet-sanity-studio.intern.nav.no/" +
    (inneholderUrl("intern.nav.no") ? "prod" : "test") +
    "/desk/tiltaksgjennomforinger;alleTiltaksgjennomforinger;";

  if (isLoadingTiltaksgjennomforing || isLoadingAvtale) {
    return <Laster tekst="Laster informasjon om tiltaksgjennomføring..." />;
  }

  if (isErrorTiltaksgjennomforing || !tiltaksgjennomforing) {
    return (
      <Alert variant="error">
        Klarte ikke hente informasjon om tiltaksgjennomføring
      </Alert>
    );
  }

  if (!tiltaksgjennomforing) {
    return <Alert variant="warning">Fant ingen tiltaksgjennomføring</Alert>;
  }

  return (
    <div className={styles.container}>
      <div className={classNames(styles.detaljer, styles.container)}>
        <dl className={styles.bolk}>
          <Metadata
            header="Tiltakstype"
            verdi={tiltaksgjennomforing.tiltakstype.navn}
          />
          <Metadata
            header="Tiltaksnummer"
            verdi={tiltaksgjennomforing.tiltaksnummer}
          />
        </dl>
        <Separator />
        <dl className={styles.bolk}>
          <Metadata
            header="Startdato"
            verdi={formaterDato(tiltaksgjennomforing.startDato)}
          />
          <Metadata
            header="Sluttdato"
            verdi={formaterDato(tiltaksgjennomforing.sluttDato)}
          />
          {Boolean(tiltaksgjennomforing.stengtFra) && 
            <Metadata
                header={
                  <Checkbox
                    checked={true}
                    description={formaterDato(tiltaksgjennomforing.stengtFra) + " - " + formaterDato(tiltaksgjennomforing.stengtTil)}
                  >
                    Midlertidig stengt
                  </Checkbox>
                }
                verdi={null}
            />
          }
        </dl>
        <Separator />
        <dl className={styles.bolk}>
          <Metadata
            header="Fylke/region"
            verdi={avtale?.navRegion?.navn || "Ingen region valgt for avtale"}
          />
          <Metadata
            header="Enhet"
            verdi={
              tiltaksgjennomforing.navEnheter.length > 0 ? (
                <ul>
                  {tiltaksgjennomforing.navEnheter.map((enhet) => (
                    <li key={enhet.enhetsnummer}>{enhet.navn}</li>
                  ))}
                </ul>
              ) : (
                "Alle enheter"
              )
            }
          />
          {tiltaksgjennomforing.virksomhetsnavn ? (
            <Metadata
              header="Arrangør"
              verdi={tiltaksgjennomforing.virksomhetsnavn}
            />
          ) : null}
        </dl>
        {tiltaksgjennomforing.sanityId ? (
          <>
            <Separator />
            <dl className={styles.bolk}>
              <Metadata
                header="Sanity dokument"
                verdi={
                  <>
                    <Link
                      target="_blank"
                      href={sanityTiltaksgjennomforingUrl + tiltaksgjennomforing.sanityId}
                    >
                      Åpne tiltaksgjennomføringen i Sanity <ExternalLinkIcon title="Åpner tiltaksgjennomføringen i Sanity" />
                    </Link>
                  </>
                }
              />
            </dl>
          </>
        ) : null}
      </div>
      <div className={styles.knapperad}>
        {features?.["mulighetsrommet.admin-flate-rediger-avtale"] ? (
          <Button
            variant="tertiary"
            onClick={handleRediger}
            data-testid="endre-avtale"
          >
            Endre
          </Button>
        ) : null}
      </div>
      <OpprettTiltaksgjennomforingModal
        modalOpen={redigerModal}
        onClose={lukkRedigerModal}
        shouldCloseOnOverlayClick={true}
        tiltaksgjennomforing={tiltaksgjennomforing}
        avtale={avtale}
      />
    </div>
  );
}
