import { PortenLink } from "@/components/PortenLink";
import { formaterDato } from "@/utils/Utils";
import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, HStack, Heading, Loader, VStack } from "@navikt/ds-react";
import { HistorikkForBruker as IHistorikkForBruker } from "mulighetsrommet-api-client";
import styles from "./HistorikkForBrukerModal.module.scss";
import { StatusBadge } from "./Statusbadge";
import { useTiltakshistorikkForBruker } from "@/api/queries/useTiltakshistorikkForBruker";

export function HistorikkForBrukerModalInnhold() {
  const { data, isLoading, isError } = useTiltakshistorikkForBruker();

  if (isLoading && !data) return <Loader />;

  if (isError) return <Alert variant="error">Kunne ikke hente brukerens tiltakshistorikk</Alert>;

  const sorterPaaFraDato = (a: IHistorikkForBruker, b: IHistorikkForBruker) => {
    if (!a.fraDato || !b.fraDato) return -1; // Flytt deltakelser uten fraDato bakerst

    return new Date(a.fraDato ?? "").getTime() - new Date(b.fraDato ?? "").getTime();
  };

  const venter = data?.filter(({ status }) => status === "VENTER").sort(sorterPaaFraDato) ?? [];
  const deltar = data?.filter(({ status }) => status === "DELTAR").sort(sorterPaaFraDato) ?? [];
  const avsluttet =
    data?.filter(({ status }) => status === "AVSLUTTET").sort(sorterPaaFraDato) ?? [];
  const ikkeAktuell =
    data?.filter(({ status }) => status === "IKKE_AKTUELL").sort(sorterPaaFraDato) ?? [];

  const tiltak = [...venter, ...deltar, ...avsluttet, ...ikkeAktuell];

  return (
    <div style={{ marginTop: "1rem" }}>
      {tiltak.length === 0 ? (
        <Alert variant="info" style={{ marginBottom: "1rem" }}>
          Vi finner ingen registrerte tiltak på brukeren
        </Alert>
      ) : null}
      <Alert variant="info" style={{ marginBottom: "1rem" }}>
        Vi viser bare tiltak 5 år tilbake i tid. Vær oppmerksom på at tiltak som er flyttet ut fra
        Arena kan mangle i historikken.
      </Alert>
      <ul className={styles.historikk_for_bruker_liste}>
        {tiltak.map((historikk) => {
          return (
            <li key={historikk.id} className={styles.historikk_for_bruker_listeelement}>
              <VStack>
                <HStack gap="10">{<small>{historikk.tiltakstype.toUpperCase()}</small>}</HStack>
                <Heading size="small" level="4">
                  {historikk.tiltaksnavn}
                </Heading>
                <HStack align={"end"} gap="5">
                  <StatusBadge status={historikk.status} />
                  {historikk.fraDato ? (
                    <BodyShort size="small">
                      {historikk.fraDato && !historikk.tilDato
                        ? `Oppstartsdato ${formaterDato(historikk.fraDato)}`
                        : [historikk.fraDato, historikk.tilDato]
                            .filter(Boolean)
                            .map((dato) => dato && formaterDato(dato))
                            .join(" - ")}
                    </BodyShort>
                  ) : null}
                </HStack>
              </VStack>
            </li>
          );
        })}
      </ul>
      <ViVilHoreFraDeg />
    </div>
  );
}

function ViVilHoreFraDeg() {
  return (
    <>
      <h4>Vi vil høre fra deg</h4>
      <BodyShort>
        Vi jobber med utvikling av historikk-funksjonaliteten og vi ønsker å høre fra deg som har
        tanker om hvordan historikken burde presenteres og fungere.{" "}
        <PortenLink>
          Send oss gjerne en melding via Porten <ExternalLinkIcon />
        </PortenLink>
      </BodyShort>
    </>
  );
}
