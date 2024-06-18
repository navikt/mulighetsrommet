import { getDisplayName } from "@/api/enhet/helpers";
import { usePollTiltaksnummer } from "@/api/tiltaksgjennomforing/usePollTiltaksnummer";
import { useMigrerteTiltakstyper } from "@/api/tiltakstyper/useMigrerteTiltakstyper";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata, Separator } from "@/components/detaljside/Metadata";
import { Laster } from "@/components/laster/Laster";
import { tiltaktekster } from "@/components/ledetekster/tiltaksgjennomforingLedetekster";
import { AvbrytGjennomforingModal } from "@/components/modal/AvbrytGjennomforingModal";
import { NokkeltallDeltakere } from "@/components/tiltaksgjennomforinger/NokkeltallDeltakere";
import { TiltakTilgjengeligForArrangor } from "@/components/tiltaksgjennomforinger/TilgjengeligTiltakForArrangor";
import { erArenaOpphavOgIngenEierskap } from "@/components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaConst";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { Kontaktperson } from "@/pages/tiltaksgjennomforinger/Kontaktperson";
import { formaterDato, formatertVentetid } from "@/utils/Utils";
import { isTiltakMedFellesOppstart } from "@/utils/tiltakskoder";
import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, HStack, HelpText, Tag } from "@navikt/ds-react";
import {
  Avtale,
  Tiltaksgjennomforing,
  TiltaksgjennomforingOppstartstype,
} from "mulighetsrommet-api-client";
import { useTitle } from "mulighetsrommet-frontend-common";
import { NOM_ANSATT_SIDE } from "mulighetsrommet-frontend-common/constants";
import { gjennomforingIsAktiv } from "mulighetsrommet-frontend-common/utils/utils";
import { useRef } from "react";
import { Link } from "react-router-dom";
import styles from "../DetaljerInfo.module.scss";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
  avtale?: Avtale;
}

export function TiltaksgjennomforingDetaljer({ tiltaksgjennomforing, avtale }: Props) {
  useTitle(
    `Tiltaksgjennomføring ${tiltaksgjennomforing.navn ? `- ${tiltaksgjennomforing.navn}` : null}`,
  );

  const { data: migrerteTiltakstyper = [] } = useMigrerteTiltakstyper();
  const avbrytModalRef = useRef<HTMLDialogElement>(null);

  const navnPaaNavEnheterForKontaktperson = (enheterForKontaktperson: string[]): string => {
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

  const kontaktpersonerFraNav = tiltaksgjennomforing.kontaktpersoner ?? [];

  const {
    tiltakstype,
    tiltaksnummer,
    startDato,
    sluttDato,
    oppstart,
    antallPlasser,
    deltidsprosent,
    apentForInnsok,
    administratorer,
    navRegion,
    navEnheter,
    arenaAnsvarligEnhet,
    arrangor,
    stedForGjennomforing,
  } = tiltaksgjennomforing;

  return (
    <>
      <div className={styles.container}>
        <div className={styles.detaljer}>
          <Bolk aria-label="Tiltaksnavn og tiltaksnummer" data-testid="tiltaksnavn">
            <Metadata header={tiltaktekster.tiltaksnavnLabel} verdi={tiltaksgjennomforing.navn} />
            <Metadata
              header={tiltaktekster.tiltaksnummerLabel}
              verdi={tiltaksnummer ?? <HentTiltaksnummer id={tiltaksgjennomforing.id} />}
            />
          </Bolk>

          <Bolk aria-label="Tiltakstype og avtaletype">
            <Metadata
              header={tiltaktekster.avtaleLabel}
              verdi={
                avtale?.id ? (
                  <>
                    <Link to={`/avtaler/${avtale.id}`}>
                      {avtale.navn} {avtale.avtalenummer ? ` - ${avtale.avtalenummer}` : null}
                    </Link>{" "}
                    <BodyShort>
                      <small>
                        Avtalens periode: {formaterDato(avtale.startDato)} -{" "}
                        {avtale?.sluttDato ? formaterDato(avtale.sluttDato) : ""}
                      </small>
                    </BodyShort>
                  </>
                ) : (
                  tiltaktekster.ingenAvtaleForGjennomforingenLabel
                )
              }
            />
            <Metadata header={tiltaktekster.tiltakstypeLabel} verdi={tiltakstype.navn} />
          </Bolk>

          <Separator />

          <Bolk aria-label={tiltaktekster.oppstartstypeLabel}>
            <Metadata
              header={tiltaktekster.oppstartstypeLabel}
              verdi={
                oppstart === TiltaksgjennomforingOppstartstype.FELLES
                  ? "Felles"
                  : "Løpende oppstart"
              }
            />
          </Bolk>
          <Bolk aria-label="Start- og sluttdato">
            <Metadata header={tiltaktekster.startdatoLabel} verdi={formaterDato(startDato)} />
            <Metadata
              header={tiltaktekster.sluttdatoLabel}
              verdi={sluttDato ? formaterDato(sluttDato) : "-"}
            />
          </Bolk>

          <Bolk>
            <Metadata header={tiltaktekster.antallPlasserLabel} verdi={antallPlasser} />
            {isTiltakMedFellesOppstart(tiltakstype.arenaKode) && (
              <Metadata header={tiltaktekster.deltidsprosentLabel} verdi={deltidsprosent} />
            )}
          </Bolk>

          <Separator />
          <Bolk aria-label={tiltaktekster.apentForInnsokLabel}>
            <Metadata
              header={tiltaktekster.apentForInnsokLabel}
              verdi={apentForInnsok ? "Ja" : "Nei"}
            />
          </Bolk>

          <Separator />

          {tiltaksgjennomforing?.estimertVentetid ? (
            <>
              <Bolk aria-label={tiltaktekster.estimertVentetidLabel}>
                <Metadata
                  header={tiltaktekster.estimertVentetidLabel}
                  verdi={formatertVentetid(
                    tiltaksgjennomforing.estimertVentetid.verdi,
                    tiltaksgjennomforing.estimertVentetid.enhet,
                  )}
                />
              </Bolk>
              <Separator />
            </>
          ) : null}

          <Bolk aria-label={tiltaktekster.administratorerForGjennomforingenLabel}>
            <Metadata
              header={tiltaktekster.administratorerForGjennomforingenLabel}
              verdi={
                administratorer?.length ? (
                  <ul>
                    {administratorer.map((admin) => {
                      return (
                        <li key={admin.navIdent}>
                          <a
                            target="_blank"
                            rel="noopener noreferrer"
                            href={`${NOM_ANSATT_SIDE}${admin?.navIdent}`}
                          >
                            {`${admin?.navn} - ${admin?.navIdent}`}{" "}
                            <ExternalLinkIcon aria-label="Ekstern lenke" />
                          </a>
                        </li>
                      );
                    })}
                  </ul>
                ) : (
                  tiltaktekster.ingenAdministratorerSattForGjennomforingenLabel
                )
              }
            />
          </Bolk>
        </div>

        <div className={styles.detaljer}>
          <Bolk aria-label={tiltaktekster.navRegionLabel}>
            <Metadata header={tiltaktekster.navRegionLabel} verdi={navRegion?.navn} />
          </Bolk>

          <Bolk aria-label={tiltaktekster.navEnheterKontorerLabel}>
            <Metadata
              header={tiltaktekster.navEnheterKontorerLabel}
              verdi={
                <ul className={styles.two_columns}>
                  {navEnheter
                    .sort((a, b) => a.navn.localeCompare(b.navn))
                    .map((enhet) => (
                      <li key={enhet.enhetsnummer}>{enhet.navn}</li>
                    ))}
                </ul>
              }
            />
          </Bolk>

          {arenaAnsvarligEnhet ? (
            <Bolk>
              <div style={{ display: "flex", gap: "1rem" }}>
                <Metadata
                  header={tiltaktekster.ansvarligEnhetFraArenaLabel}
                  verdi={getDisplayName(arenaAnsvarligEnhet)}
                />
                <HelpText title="Hva betyr feltet 'Ansvarlig enhet fra Arena'?">
                  Ansvarlig enhet fra Arena blir satt i Arena basert på tiltaksansvarlig sin enhet
                  når man oppretter tiltak i Arena.
                </HelpText>
              </div>
            </Bolk>
          ) : null}

          {kontaktpersonerFraNav.map((kp, index) => {
            return (
              <Bolk key={index} classez={styles.nav_kontaktpersoner}>
                <Metadata
                  header={
                    <>
                      <span style={{ fontWeight: "bold" }}>Kontaktperson for:</span>{" "}
                      <span style={{ fontWeight: "initial" }}>
                        {navnPaaNavEnheterForKontaktperson(
                          kp.navEnheter.sort((a, b) => a.localeCompare(b)),
                        )}
                      </span>
                    </>
                  }
                  verdi={<Kontaktperson kontaktperson={kp} />}
                />
              </Bolk>
            );
          })}

          <Separator />

          {avtale?.arrangor ? (
            <Bolk aria-label={tiltaktekster.tiltaksarrangorHovedenhetLabel}>
              <Metadata
                header={tiltaktekster.tiltaksarrangorHovedenhetLabel}
                verdi={
                  <Link to={`/arrangorer/${avtale.arrangor.id}`}>
                    {avtale.arrangor.navn} - {avtale.arrangor.organisasjonsnummer}
                  </Link>
                }
              />
            </Bolk>
          ) : null}

          {arrangor ? (
            <Bolk aria-label={tiltaktekster.tiltaksarrangorUnderenhetLabel}>
              <Metadata
                header={tiltaktekster.tiltaksarrangorUnderenhetLabel}
                verdi={`${arrangor.navn} - ${arrangor.organisasjonsnummer}`}
              />
            </Bolk>
          ) : null}
          {arrangor.kontaktpersoner.length > 0 && (
            <Metadata
              header={tiltaktekster.kontaktpersonerHosTiltaksarrangorLabel}
              verdi={
                <div className={styles.arrangor_kontaktinfo_container}>
                  {arrangor.kontaktpersoner.map((kontaktperson) => (
                    <ArrangorKontaktpersonDetaljer
                      key={kontaktperson.id}
                      kontaktperson={kontaktperson}
                    />
                  ))}
                </div>
              }
            />
          )}
          {stedForGjennomforing && (
            <>
              <Separator />
              <Bolk aria-label={tiltaktekster.stedForGjennomforingLabel}>
                <Metadata
                  header={tiltaktekster.stedForGjennomforingLabel}
                  verdi={stedForGjennomforing}
                />
              </Bolk>
            </>
          )}
          <TiltakTilgjengeligForArrangor gjennomforing={tiltaksgjennomforing} />
        </div>
      </div>
      {!erArenaOpphavOgIngenEierskap(tiltaksgjennomforing, migrerteTiltakstyper) &&
        gjennomforingIsAktiv(tiltaksgjennomforing.status.status) && (
          <>
            <HarSkrivetilgang ressurs="Tiltaksgjennomføring">
              <Button
                size="small"
                variant="danger"
                onClick={() => avbrytModalRef.current?.showModal()}
              >
                Avbryt gjennomføring
              </Button>
            </HarSkrivetilgang>
            <AvbrytGjennomforingModal
              modalRef={avbrytModalRef}
              tiltaksgjennomforing={tiltaksgjennomforing}
            />
          </>
        )}
      <NokkeltallDeltakere tiltaksgjennomforingId={tiltaksgjennomforing.id} />
    </>
  );
}

function HentTiltaksnummer({ id }: { id: string }) {
  const { isError, isLoading, data } = usePollTiltaksnummer(id);
  return isError ? (
    <Tag variant="error">Klarte ikke hente tiltaksnummer</Tag>
  ) : isLoading ? (
    <HStack align={"center"} gap="1">
      <Laster />
      <span>Henter tiltaksnummer i Arena</span>
    </HStack>
  ) : (
    data?.tiltaksnummer
  );
}
