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
import { SokeSelect } from "mulighetsrommet-frontend-common/components/SokeSelect";

export const SanityPreviewOversikt = () => {
  const [geografiskEnhet, setGeografiskEnhet] = useState<NavEnhet | undefined>();
  const {
    data: tiltaksgjennomforinger = [],
    isLoading,
    isFetching,
    refetch,
  } = usePreviewTiltaksgjennomforinger(geografiskEnhet?.enhetsnummer);
  const { data: enheter } = useNavEnheter();

  useEffect(() => {
    if (geografiskEnhet) {
      refetch();
    }
  }, [geografiskEnhet]);

  if (!enheter || !tiltaksgjennomforinger) {
    return <Loader />;
  }

  return (
    <>
      <SokeSelect
        label="Brukers geografiske enhet"
        description="Kun ment som hjelp til forhåndsvisning"
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
