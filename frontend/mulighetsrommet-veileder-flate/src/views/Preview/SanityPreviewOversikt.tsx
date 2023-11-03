import Filtermeny from "../../components/filtrering/Filtermeny";
import Tiltaksgjennomforingsoversikt from "../../components/oversikt/Tiltaksgjennomforingsoversikt";
import styles from "../tiltaksgjennomforing-oversikt/ViewTiltaksgjennomforingOversikt.module.scss";
import { NavEnhet, NavEnhetType } from "mulighetsrommet-api-client";
import { usePreviewTiltaksgjennomforinger } from "../../core/api/queries/usePreviewTiltaksgjennomforinger";
import { Loader } from "@navikt/ds-react";
import { useNavEnheter } from "../../core/api/queries/useNavEnheter";
import { useEffect, useState } from "react";
import { Separator } from "../../utils/Separator";
import { Feilmelding } from "../../components/feilmelding/Feilmelding";
import { SokeSelect } from "../../components/sokeselect/SokeSelect";

export const SanityPreviewOversikt = () => {
  const [geografiskEnhet, setGeografiskEnhet] = useState<NavEnhet>();
  const [oppfolgingsenhet, setOppfolgingsenhet] = useState<NavEnhet>();
  const {
    data: tiltaksgjennomforinger = [],
    isLoading,
    isFetching,
    refetch,
  } = usePreviewTiltaksgjennomforinger(geografiskEnhet?.enhetsnummer);
  const { data: enheter } = useNavEnheter();

  useEffect(() => {
    refetch();
  }, [geografiskEnhet]);

  if (!enheter || !tiltaksgjennomforinger) {
    return <Loader />;
  }

  return (
    <>
      <div style={{ display: "flex", gap: "2rem" }}>
        <SokeSelect
          label="Brukers geografiske enhet"
          description="Simuler en brukers geografiske enhet"
          value={
            geografiskEnhet !== undefined
              ? { label: geografiskEnhet.navn, value: geografiskEnhet.enhetsnummer }
              : null
          }
          name="geografisk-enhet"
          size="medium"
          onChange={(e) =>
            setGeografiskEnhet(
              enheter.find((enhet: NavEnhet) => enhet.enhetsnummer === e.target.value),
            )
          }
          placeholder="Velg brukers geografiske enhet"
          options={enheter
            .filter((enhet: NavEnhet) => enhet.type === NavEnhetType.LOKAL)
            .map((enhet: NavEnhet) => ({
              label: enhet.navn,
              value: enhet.enhetsnummer,
            }))}
        />
        <SokeSelect
          label="Brukers oppfølgingsenhet"
          description="Simuler en brukers oppfølgingsenhet"
          value={
            oppfolgingsenhet !== undefined
              ? { label: oppfolgingsenhet.navn, value: oppfolgingsenhet.enhetsnummer }
              : null
          }
          name="oppfolgingsenhet"
          size="medium"
          onChange={(e) =>
            setOppfolgingsenhet(
              enheter.find((enhet: NavEnhet) => enhet.enhetsnummer === e.target.value),
            )
          }
          placeholder="Velg brukers oppfølgingsenhet"
          options={enheter
            .filter(
              (enhet: NavEnhet) =>
                enhet.type !== NavEnhetType.LOKAL && enhet.type !== NavEnhetType.FYLKE,
            )
            .map((enhet: NavEnhet) => ({
              label: enhet.navn,
              value: enhet.enhetsnummer,
            }))}
        />
      </div>
      <Separator />

      <div className={styles.tiltakstype_oversikt} data-testid="tiltakstype-oversikt">
        <Filtermeny />
        <div>
          {isLoading ? (
            <div className={styles.filter_loader}>
              <Loader />
            </div>
          ) : tiltaksgjennomforinger.length === 0 ? (
            <Feilmelding
              header="Ingen tiltaksgjennomføringer funnet"
              beskrivelse="Prøv å justere søket eller filteret for å finne det du leter etter"
              ikonvariant="warning"
            />
          ) : (
            <Tiltaksgjennomforingsoversikt
              tiltaksgjennomforinger={tiltaksgjennomforinger}
              isFetching={isFetching}
            />
          )}
        </div>
      </div>
    </>
  );
};
