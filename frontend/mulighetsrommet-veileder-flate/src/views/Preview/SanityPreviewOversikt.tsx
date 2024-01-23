import Filtermeny from "../../components/filtrering/Filtermeny";
import Tiltaksgjennomforingsoversikt from "../../components/oversikt/Tiltaksgjennomforingsoversikt";
import styles from "../tiltaksgjennomforing-oversikt/ViewTiltaksgjennomforingOversikt.module.scss";
import { Loader } from "@navikt/ds-react";
import { Feilmelding } from "../../components/feilmelding/Feilmelding";
import { FilterAndTableLayout } from "../../components/filtrering/FilterAndTableLayout";
import useTiltaksgjennomforinger from "../../core/api/queries/useTiltaksgjennomforinger";

export const SanityPreviewOversikt = () => {
  const { data: tiltaksgjennomforinger = [], isLoading } = useTiltaksgjennomforinger();

  return (
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
  );
};
