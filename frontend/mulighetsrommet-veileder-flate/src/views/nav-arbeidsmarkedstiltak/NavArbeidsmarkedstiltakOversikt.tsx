import Filtermeny from "../../components/filtrering/Filtermeny";
import Tiltaksgjennomforingsoversikt from "../../components/oversikt/Tiltaksgjennomforingsoversikt";
import { Feilmelding } from "../../components/feilmelding/Feilmelding";
import { FilterAndTableLayout } from "../../components/filtrering/FilterAndTableLayout";
import { useNavTiltaksgjennomforinger } from "../../core/api/queries/useTiltaksgjennomforinger";
import { TiltakLoader } from "../../components/TiltakLoader";

export const NavArbeidsmarkedstiltakOversikt = () => {
  const { data: tiltaksgjennomforinger = [], isLoading } = useNavTiltaksgjennomforinger();

  return (
    <FilterAndTableLayout
      buttons={null}
      filter={<Filtermeny />}
      tags={null}
      table={
        <div>
          {isLoading ? (
            <TiltakLoader />
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
