import {
  ExclamationmarkTriangleIcon,
  ExternalLinkIcon,
} from "@navikt/aksel-icons";
import { Alert, Heading } from "@navikt/ds-react";
import {
  TiltaksgjennomforingOppstartstype,
  TiltaksgjennomforingStatus,
} from "mulighetsrommet-api-client";
import { NOM_ANSATT_SIDE } from "mulighetsrommet-frontend-common/constants";
import { useState } from "react";
import invariant from "tiny-invariant";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Bolk } from "../../components/detaljside/Bolk";
import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { VisHvisVerdi } from "../../components/detaljside/VisHvisVerdi";
import { Laster } from "../../components/laster/Laster";
import {
  erDevMiljo,
  formaterDato,
  inneholderUrl,
  tilgjengelighetsstatusTilTekst,
} from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";
import { TiltaksgjennomforingKnapperad } from "./TiltaksgjennomforingKnapperad";
import { Kontaktperson } from "./Kontaktperson";
import SlettAvtaleGjennomforingModal from "../../components/modal/SlettAvtaleGjennomforingModal";
import { useDeleteTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useDeleteTiltaksgjennomforing";
import { Link } from "react-router-dom";

export function TiltaksgjennomforingInfo() {
  const {
    data: tiltaksgjennomforing,
    isError: isErrorTiltaksgjennomforing,
    isLoading: isLoadingTiltaksgjennomforing,
  } = useTiltaksgjennomforingById();

  const { data: avtale, isLoading: isLoadingAvtale } = useAvtale(
    tiltaksgjennomforing?.avtaleId,
  );

  const forhandsvisningMiljo =
    import.meta.env.dev || erDevMiljo ? "dev.nav.no" : "nav.no";

  const [slettModal, setSlettModal] = useState(false);
  const mutation = useDeleteTiltaksgjennomforing();

  const navnPaaNavEnheterForKontaktperson = (
    enheterForKontaktperson: string[],
  ): string => {
    return (
      tiltaksgjennomforing?.navEnheter
        .map((enhet) => {
          return enheterForKontaktperson
            .map((kp) => {
              if (enhet.enhetsnummer === kp) {
                return enhet.navn;
              }
              return null;
            })
            .join("");
        })
        .filter(Boolean)
        .join(", ") || "alle enheter"
    );
  };

  const sanityTiltaksgjennomforingUrl =
    "https://mulighetsrommet-sanity-studio.intern.nav.no/" +
    (inneholderUrl("intern.nav.no") ? "prod" : "test") +
    "/desk/tiltaksgjennomforinger;alleTiltaksgjennomforinger;";

  if (isLoadingTiltaksgjennomforing && isLoadingAvtale) {
    return <Laster tekst="Laster informasjon om tiltaksgjennomføring..." />;
  }

  if (isErrorTiltaksgjennomforing) {
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

  invariant(
    tiltaksgjennomforing?.status,
    "Klarte ikke finne status for tiltaksgjennomføringen",
  );

  function visKnapperad(status: TiltaksgjennomforingStatus): boolean {
    const whitelist: TiltaksgjennomforingStatus[] = [
      TiltaksgjennomforingStatus.GJENNOMFORES,
      TiltaksgjennomforingStatus.APENT_FOR_INNSOK,
    ];

    return whitelist.includes(status);
  }

  return (
    <>
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
                    <Link to={`/avtaler/${avtale?.id}`}>
                      {avtale?.navn}{" "}
                      {avtale?.avtalenummer
                        ? ` - ${avtale.avtalenummer}`
                        : null}
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
                tiltaksgjennomforing.tilgjengelighet,
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

          <Bolk aria-label="Administrator for gjennomføringen">
            <Metadata
              header="Administrator for gjennomføringen"
              verdi={
                tiltaksgjennomforing.administrator?.navIdent ? (
                  <a
                    target="_blank"
                    rel="noopener noreferrer"
                    href={`${NOM_ANSATT_SIDE}${tiltaksgjennomforing.administrator?.navIdent}`}
                  >
                    {`${tiltaksgjennomforing.administrator?.navn} - ${tiltaksgjennomforing.administrator?.navIdent}`}{" "}
                    <ExternalLinkIcon />
                  </a>
                ) : (
                  "Ingen administrator satt for gjennomføringen"
                )
              }
            />
          </Bolk>

          <VisHvisVerdi verdi={tiltaksgjennomforing.sanityId}>
            <Separator />
            <Bolk aria-label="Sanity-dokument">
              <Metadata
                header="Sanity dokument"
                verdi={
                  <>
                    <Link
                      target="_blank"
                      to={
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
          </VisHvisVerdi>
          <VisHvisVerdi verdi={tiltaksgjennomforing.sanityId}>
            <Bolk aria-label="Forhåndsvisning">
              <Metadata
                header="Forhåndsvisning i veilederflate (Modia)"
                verdi={
                  <>
                    <Link
                      target="_blank"
                      to={`https://mulighetsrommet-veileder-flate.intern.${forhandsvisningMiljo}/preview/${tiltaksgjennomforing.sanityId}?preview=true`}
                    >
                      Forhåndsvis gjennomføringen{" "}
                      <ExternalLinkIcon title="Forhåndsviser tiltaksgjennomføringen i veilederflate (Modia)" />
                    </Link>
                  </>
                }
              />
            </Bolk>
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
                <ul>
                  {tiltaksgjennomforing.navEnheter.map((enhet) => (
                    <li key={enhet.enhetsnummer}>{enhet.navn}</li>
                  ))}
                </ul>
              }
            />
          </Bolk>
          {kontaktpersonerFraNav.map((kp, index) => {
            return (
              <Bolk
                aria-label={`Kontaktperson hos ${navnPaaNavEnheterForKontaktperson(
                  kp.navEnheter,
                )}`}
                key={index}
              >
                <Metadata
                  header={`Kontaktperson hos ${navnPaaNavEnheterForKontaktperson(
                    kp.navEnheter,
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

          <VisHvisVerdi verdi={tiltaksgjennomforing.arrangor.navn}>
            <Bolk aria-label="Arrangør underenhet">
              <Metadata
                header="Arrangør underenhet"
                verdi={`${tiltaksgjennomforing.arrangor.navn} - ${tiltaksgjennomforing.arrangor.organisasjonsnummer}`}
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

          {tiltaksgjennomforing.arrangor.kontaktperson && (
            <Metadata
              header="Kontaktperson hos arrangør"
              verdi={
                <div className={styles.leverandor_kontaktinfo}>
                  <label>
                    {tiltaksgjennomforing.arrangor.kontaktperson?.navn}
                  </label>
                  <label>
                    {tiltaksgjennomforing.arrangor.kontaktperson?.telefon}
                  </label>
                  <a
                    href={`mailto:${tiltaksgjennomforing.arrangor.kontaktperson?.epost}`}
                  >
                    {tiltaksgjennomforing.arrangor.kontaktperson?.epost}
                  </a>
                  {tiltaksgjennomforing.arrangor.kontaktperson?.beskrivelse && (
                    <label>
                      {tiltaksgjennomforing.arrangor.kontaktperson?.beskrivelse}
                    </label>
                  )}
                </div>
              }
            />
          )}
        </div>

        {visKnapperad(tiltaksgjennomforing.status) ? (
          <TiltaksgjennomforingKnapperad
            handleSlett={() => setSlettModal(true)}
          />
        ) : null}
        <SlettAvtaleGjennomforingModal
          modalOpen={slettModal}
          handleCancel={() => setSlettModal(false)}
          data={tiltaksgjennomforing}
          mutation={mutation}
          dataType={"tiltaksgjennomforing"}
        />
      </div>
    </>
  );
}
