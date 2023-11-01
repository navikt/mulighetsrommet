import Filtermeny from "../../components/filtrering/Filtermeny";
import Tiltaksgjennomforingsoversikt from "../../components/oversikt/Tiltaksgjennomforingsoversikt";
import styles from "../tiltaksgjennomforing-oversikt/ViewTiltaksgjennomforingOversikt.module.scss";
import { NavEnhet, NavEnhetType } from "mulighetsrommet-api-client";
import { usePreviewTiltaksgjennomforinger } from "../../core/api/queries/usePreviewTiltaksgjennomforinger";
import { Loader } from "@navikt/ds-react";
import { useNavEnheter } from "../../core/api/queries/useNavEnheter";
import { SokeSelect } from "mulighetsrommet-frontend-common";
import { useEffect, useState } from "react";
import { Separator } from "../../utils/Separator";
import { TilbakestillFilterFeil } from "../tiltaksgjennomforing-oversikt/ViewTiltaksgjennomforingOversikt";

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
    refetch();
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
            <TilbakestillFilterFeil />
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
