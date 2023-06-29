import {
  ExclamationmarkTriangleIcon,
  ExternalLinkIcon,
} from "@navikt/aksel-icons";
import { Alert, Button, Heading, Link } from "@navikt/ds-react";
import classNames from "classnames";
import { useState } from "react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { Laster } from "../../components/laster/Laster";
import { OpprettTiltaksgjennomforingModal } from "../../components/modal/OpprettTiltaksgjennomforingModal";
import SlettTiltaksgjennomforingModal from "../../components/tiltaksgjennomforinger/SlettTiltaksgjennomforingModal";
import { TiltaksgjennomforingOppstartstype } from "mulighetsrommet-api-client";
import {
  formaterDato,
  inneholderUrl,
  tilgjengelighetsstatusTilTekst,
} from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";

const TEAMS_DYPLENKE = "https://teams.microsoft.com/l/chat/0/0?users=";

export function TiltaksgjennomforingInfo() {
  const {
    data: tiltaksgjennomforing,
    isError: isErrorTiltaksgjennomforing,
    isLoading: isLoadingTiltaksgjennomforing,
    refetch,
  } = useTiltaksgjennomforingById();
  const { data: avtale, isLoading: isLoadingAvtale } = useAvtale(
    tiltaksgjennomforing?.avtaleId
  );
  const { data: features } = useFeatureToggles();

  const [slettModal, setSlettModal] = useState(false);
  const [redigerModal, setRedigerModal] = useState(false);

  const handleRediger = () => setRedigerModal(true);
  const lukkRedigerModal = () => setRedigerModal(false);
  const handleSlett = () => setSlettModal(true);
  const lukkSlettModal = () => setSlettModal(false);

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

  const todayDate = new Date();
  const kontaktpersonerFraNav = tiltaksgjennomforing.kontaktpersoner ?? [];

  return (
    <div className={styles.container}>
      <div className={classNames(styles.detaljer, styles.container)}>
        <dl className={styles.bolk}>
          <Metadata
            header="Tiltakstype"
            verdi={tiltaksgjennomforing.tiltakstype.navn}
          />
          {tiltaksgjennomforing.tiltaksnummer ? (
            <Metadata
              header="Tiltaksnummer"
              verdi={tiltaksgjennomforing.tiltaksnummer}
            />
          ) : null}
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
          <Metadata
            header="Oppstart"
            verdi={
              tiltaksgjennomforing.oppstart ===
              TiltaksgjennomforingOppstartstype.FELLES
                ? "Felles"
                : "Løpende oppstart"
            }
          />
          {Boolean(tiltaksgjennomforing.stengtFra) &&
            Boolean(tiltaksgjennomforing.stengtTil) &&
            new Date(tiltaksgjennomforing.stengtTil!!) > todayDate && (
              <Metadata
                header={
                  todayDate >= new Date(tiltaksgjennomforing.stengtFra!!) &&
                  todayDate <= new Date(tiltaksgjennomforing.stengtTil!!) ? (
                    <div style={{ display: "flex", flexDirection: "row" }}>
                      <ExclamationmarkTriangleIcon
                        style={{ marginRight: "5px" }}
                        title="midlertidig-stengt"
                      />
                      <Heading size="xsmall" level="2">
                        Midlertidig Stengt
                      </Heading>
                    </div>
                  ) : (
                    <Heading size="xsmall" level="2">
                      Midlertidig Stengt
                    </Heading>
                  )
                }
                verdi={
                  formaterDato(tiltaksgjennomforing.stengtFra) +
                  " - " +
                  formaterDato(tiltaksgjennomforing.stengtTil)
                }
              />
            )}
        </dl>
        <Separator />
        <dl className={styles.bolk}>
          <Metadata
            header="Tilgjengelighetsstatus"
            verdi={tilgjengelighetsstatusTilTekst(
              tiltaksgjennomforing.tilgjengelighet
            )}
          />
          {tiltaksgjennomforing?.estimertVentetid ? (
            <Metadata
              header="Estimert ventetid"
              verdi={tiltaksgjennomforing.estimertVentetid}
            />
          ) : null}
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
          {tiltaksgjennomforing.arrangorNavn ? (
            <Metadata
              header="Arrangør"
              verdi={tiltaksgjennomforing.arrangorNavn}
            />
          ) : null}
          {tiltaksgjennomforing.arrangorKontaktperson &&
            <Metadata
              header="Kontaktperson"
              verdi={
                <div className={styles.leverandor_kontaktinfo}>
                  <label>{tiltaksgjennomforing.arrangorKontaktperson?.navn}</label>
                  <label>{tiltaksgjennomforing.arrangorKontaktperson?.telefon}</label>
                  <a href={`mailto:${tiltaksgjennomforing.arrangorKontaktperson?.epost}`}>{tiltaksgjennomforing.arrangorKontaktperson?.epost}</a>
                </div>
              }
            />
          }
          {tiltaksgjennomforing.lokasjonArrangor ? (
            <Metadata
              header="Lokasjon for gjennomføring"
              verdi={tiltaksgjennomforing.lokasjonArrangor}
            />
          ) : null}
        </dl>
        {kontaktpersonerFraNav.length > 0 ? (
          <>
            <Separator />
            <dl className={styles.bolk}>
              <Metadata
                header="Kontaktpersoner i NAV"
                verdi={
                  kontaktpersonerFraNav.length > 0 ? (
                    <ul>
                      {kontaktpersonerFraNav.map((kontakt) => (
                        <li key={kontakt.navIdent}>
                          {kontakt.navn}{" "}
                          {kontakt.epost ? (
                            <span>
                              -{" "}
                              <a
                                href={`${TEAMS_DYPLENKE}${encodeURIComponent(
                                  kontakt.epost
                                )}`}
                              >
                                Ta kontakt på Teams
                              </a>
                            </span>
                          ) : null}
                        </li>
                      ))}
                    </ul>
                  ) : (
                    "Alle enheter"
                  )
                }
              />
            </dl>
          </>
        ) : null}
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
                      href={
                        sanityTiltaksgjennomforingUrl +
                        tiltaksgjennomforing.sanityId
                      }
                    >
                      Åpne tiltaksgjennomføringen i Sanity{" "}
                      <ExternalLinkIcon title="Åpner tiltaksgjennomføringen i Sanity" />
                    </Link>
                  </>
                }
              />
            </dl>
          </>
        ) : null}
      </div>
      <div className={styles.knapperad}>
        {features?.[
          "mulighetsrommet.admin-flate-slett-tiltaksgjennomforing"
        ] ? (
          <Button
            variant="tertiary-neutral"
            onClick={handleSlett}
            data-testid="slett-gjennomforing"
            className={styles.slett_knapp}
          >
            Feilregistrering
          </Button>
        ) : null}
        {features?.[
          "mulighetsrommet.admin-flate-rediger-tiltaksgjennomforing"
        ] ? (
          <Button
            variant="tertiary"
            onClick={handleRediger}
            data-testid="endre-gjennomforing"
          >
            Endre
          </Button>
        ) : null}
      </div>
      <OpprettTiltaksgjennomforingModal
        modalOpen={redigerModal}
        onClose={lukkRedigerModal}
        onSuccess={() => {
          lukkRedigerModal();
          refetch();
        }}
        tiltaksgjennomforing={tiltaksgjennomforing}
        avtale={avtale}
      />
      <SlettTiltaksgjennomforingModal
        modalOpen={slettModal}
        onClose={lukkSlettModal}
        shouldCloseOnOverlayClick={true}
        tiltaksgjennomforing={tiltaksgjennomforing}
        handleRediger={() => setRedigerModal(true)}
      />
    </div>
  );
}
