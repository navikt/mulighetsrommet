import Filtermeny from "../../components/filtrering/Filtermeny";
import Tiltaksgjennomforingsoversikt from "../../components/oversikt/Tiltaksgjennomforingsoversikt";
import { NavEnhet, NavEnhetType } from "mulighetsrommet-api-client";
import { usePreviewTiltaksgjennomforinger } from "../../core/api/queries/usePreviewTiltaksgjennomforinger";
import { Loader } from "@navikt/ds-react";
import { useNavEnheter } from "../../core/api/queries/useNavEnheter";
import { useEffect } from "react";
import { Separator } from "../../utils/Separator";
import { Feilmelding } from "../../components/feilmelding/Feilmelding";
import { SokeSelect } from "mulighetsrommet-frontend-common/components/SokeSelect";
import { useAtom } from "jotai";
import { geografiskEnhetForPreviewAtom } from "../../core/atoms/atoms";
import { FilterAndTableLayout } from "../../components/filtrering/FilterAndTableLayout";
import styles from "./PreviewView.module.scss";

export const PreviewOversikt = () => {
  const [geografiskEnhet, setGeografiskEnhet] = useAtom(geografiskEnhetForPreviewAtom);
  const {
    data: tiltaksgjennomforinger = [],
    isLoading,
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
      <div style={{ maxWidth: "21rem" }}>
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
      </div>
      <Separator />
      <FilterAndTableLayout
        buttons={null}
        filter={<Filtermeny />}
        tags={null}
        table={
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
              <Tiltaksgjennomforingsoversikt tiltaksgjennomforinger={tiltaksgjennomforinger} />
            )}
          </div>
        }
      />
    </>
  );
};
