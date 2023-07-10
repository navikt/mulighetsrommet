import {
  ExclamationmarkTriangleIcon,
  ExternalLinkIcon,
} from "@navikt/aksel-icons";
import { Alert, Button, Heading, Link } from "@navikt/ds-react";
import {
  TiltaksgjennomforingKontaktpersoner,
  TiltaksgjennomforingOppstartstype,
} from "mulighetsrommet-api-client";
import { useState } from "react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Bolk } from "../../components/detaljside/Bolk";
import {
  Liste,
  Metadata,
  Separator,
} from "../../components/detaljside/Metadata";
import { VisHvisVerdi } from "../../components/detaljside/VisHvisVerdi";
import { Laster } from "../../components/laster/Laster";
import { OpprettTiltaksgjennomforingModal } from "../../components/modal/OpprettTiltaksgjennomforingModal";
import SlettTiltaksgjennomforingModal from "../../components/tiltaksgjennomforinger/SlettTiltaksgjennomforingModal";
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

  const navnPaaNavEnheterForKontaktperson = (
    enheterForKontaktperson: string[]
  ): string => {
    return (
      tiltaksgjennomforing?.navEnheter
        .map((enhet) => {
          const enhetNavn = enheterForKontaktperson
            .map((kp) => {
              if (enhet.enhetsnummer === kp) {
                return enhet.navn;
              }
              return null;
            })
            .join("");
          return enhetNavn;
        })
        .filter(Boolean)
        .join(", ") || ""
    );
  };

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
      <div className={styles.detaljer}>
        <Bolk aria-label="Tiltakstype">
          <Metadata
            header="Tiltakstype"
            verdi={tiltaksgjennomforing.tiltakstype.navn}
          />
          <VisHvisVerdi verdi={tiltaksgjennomforing.tiltaksnummer}>
            <Bolk aria-label="Tiltaksnummer">
              <Metadata
                header="Tiltaksnummer"
                verdi={tiltaksgjennomforing.tiltaksnummer}
              />
            </Bolk>
          </VisHvisVerdi>
        </Bolk>
        <Bolk aria-label="Avtale">
          <Metadata
            header="Avtale"
            verdi={
              avtale?.id ? (
                <>
                  <Link href={`/avtaler/${avtale?.id}`}>
                    {avtale?.navn}{" "}
                    {avtale?.avtalenummer ? ` - ${avtale.avtalenummer}` : null}
                  </Link>{" "}
                </>
              ) : (
                "Ingen avtale for gjennomføringen"
              )
            }
          />
        </Bolk>
        <Separator />
        <Bolk aria-label="Start- og sluttdato">
          <Metadata
            header="Startdato"
            verdi={formaterDato(tiltaksgjennomforing.startDato)}
          />
          <Metadata
            header="Sluttdato"
            verdi={formaterDato(tiltaksgjennomforing.sluttDato)}
          />
        </Bolk>
        <Bolk aria-label="Oppstartsdato">
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
                      <Heading size="xsmall" level="3">
                        Midlertidig Stengt
                      </Heading>
                    </div>
                  ) : (
                    <Heading size="xsmall" level="3">
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
        </Bolk>
        <Bolk aria-label="Antall plasser">
          <Metadata
            header="Antall plasser"
            verdi={tiltaksgjennomforing.antallPlasser}
          />
        </Bolk>
        <Separator />
        <Bolk aria-label="Tilgjengelighetsstatus">
          <Metadata
            header="Tilgjengelighetsstatus"
            verdi={tilgjengelighetsstatusTilTekst(
              tiltaksgjennomforing.tilgjengelighet
            )}
          />
          <VisHvisVerdi verdi={tiltaksgjennomforing.estimertVentetid}>
            <Metadata
              header="Estimert ventetid"
              verdi={tiltaksgjennomforing.estimertVentetid}
            />
          </VisHvisVerdi>
        </Bolk>
        <Separator />
        <Bolk aria-label="Anvarlig for gjennomføringen">
          <Metadata
            header="Ansvarlig for gjennomføringen"
            verdi={
              tiltaksgjennomforing.ansvarlig?.navident ? (
                <a
                  target="_blank"
                  rel="noopener noreferrer"
                  href={`https://nom.nav.no/ressurs/${tiltaksgjennomforing.ansvarlig?.navident}`}
                >
                  {`${tiltaksgjennomforing.ansvarlig?.navn} - ${tiltaksgjennomforing.ansvarlig?.navident}`}{" "}
                  <ExternalLinkIcon />
                </a>
              ) : (
                "Ingen ansvarlig satt for gjennomføringen"
              )
            }
          />
        </Bolk>
        <VisHvisVerdi verdi={tiltaksgjennomforing.sanityId}>
          <>
            <Separator />
            <Bolk aria-label="Sanity-dokument">
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
            </Bolk>
          </>
        </VisHvisVerdi>
      </div>
      <div className={styles.detaljer}>
        <Bolk aria-label="NAV-region">
          <Metadata header="NAV-region" verdi={avtale?.navRegion?.navn} />
        </Bolk>
        <Bolk aria-label="Nav-enheter">
          <Metadata
            header="Nav-enhet (kontorer)"
            verdi={
              <Liste
                elementer={tiltaksgjennomforing.navEnheter.map((enhet) => ({
                  key: enhet.enhetsnummer,
                  value: enhet.navn,
                }))}
                tekstHvisTom="Alle enheter"
              />
            }
          />
        </Bolk>
        {kontaktpersonerFraNav.map((kp, index) => {
          return (
            <Bolk
              aria-label={`Kontaktperson hos ${navnPaaNavEnheterForKontaktperson(
                kp.navEnheter
              )}`}
              key={index}
            >
              <Metadata
                header={`Kontaktperson hos ${navnPaaNavEnheterForKontaktperson(
                  kp.navEnheter
                )}`}
                verdi={<Kontaktperson kontaktperson={kp} />}
              />
            </Bolk>
          );
        })}

        <Separator />

        <VisHvisVerdi verdi={avtale?.leverandor}>
          <Bolk aria-label="Tiltaksleverandør hovedenhet">
            <Metadata
              header="Tiltaksleverandør hovedenhet"
              verdi={[
                avtale?.leverandor.navn,
                avtale?.leverandor.organisasjonsnummer,
              ]
                .filter(Boolean)
                .join(" - ")}
            />
          </Bolk>
        </VisHvisVerdi>
        <VisHvisVerdi verdi={tiltaksgjennomforing.arrangorNavn}>
          <Bolk aria-label="Arrangør underenhet">
            <Metadata
              header="Arrangør underenhet"
              verdi={`${tiltaksgjennomforing.arrangorNavn} - ${tiltaksgjennomforing.arrangorOrganisasjonsnummer}`}
            />
          </Bolk>
        </VisHvisVerdi>

        <VisHvisVerdi verdi={tiltaksgjennomforing.lokasjonArrangor}>
          <Bolk aria-label="Sted for gjennomføringen">
            <Metadata
              header="Sted for gjennomføringen"
              verdi={tiltaksgjennomforing.lokasjonArrangor}
            />
          </Bolk>
        </VisHvisVerdi>

        {tiltaksgjennomforing.arrangorKontaktperson && (
          <Metadata
            header="Kontaktperson hos arrangør"
            verdi={
              <div className={styles.leverandor_kontaktinfo}>
                <label>
                  {tiltaksgjennomforing.arrangorKontaktperson?.navn}
                </label>
                <label>
                  {tiltaksgjennomforing.arrangorKontaktperson?.telefon}
                </label>
                <a
                  href={`mailto:${tiltaksgjennomforing.arrangorKontaktperson?.epost}`}
                >
                  {tiltaksgjennomforing.arrangorKontaktperson?.epost}
                </a>
              </div>
            }
          />
        )}
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

interface KontaktpersonProps {
  kontaktperson: TiltaksgjennomforingKontaktpersoner;
}

function Kontaktperson({ kontaktperson }: KontaktpersonProps) {
  return (
    <div className={styles.leverandor_kontaktinfo}>
      <label>{kontaktperson.navn}</label>
      <label>{kontaktperson.mobilnummer}</label>
      <a href={`${TEAMS_DYPLENKE}${kontaktperson.epost}`}>
        {kontaktperson.epost}
      </a>
    </div>
  );
}
