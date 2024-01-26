import Filtermeny from "../../components/filtrering/Filtermeny";
import Tiltaksgjennomforingsoversikt from "../../components/oversikt/Tiltaksgjennomforingsoversikt";
import { Loader } from "@navikt/ds-react";
import { Feilmelding } from "../../components/feilmelding/Feilmelding";
import { FilterAndTableLayout } from "../../components/filtrering/FilterAndTableLayout";
import useTiltaksgjennomforinger from "../../core/api/queries/useTiltaksgjennomforinger";
import styles from "./PreviewView.module.scss";
import { Filtertags } from "../../components/filtrering/Filtertags";

export const PreviewOversikt = () => {
  const { data: tiltaksgjennomforinger = [], isLoading } = useTiltaksgjennomforinger();

  return (
    <FilterAndTableLayout
      buttons={null}
      filter={<Filtermeny />}
      tags={<Filtertags />}
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
  );
};
