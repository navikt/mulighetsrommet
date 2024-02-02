import { TiltakLoader } from "@/components/TiltakLoader";
import { Feilmelding } from "@/components/feilmelding/Feilmelding";
import { FilterAndTableLayout } from "@/components/filtrering/FilterAndTableLayout";
import { Filtertags } from "@/components/filtrering/Filtertags";
import { Tiltaksgjennomforingsoversikt } from "@/components/oversikt/Tiltaksgjennomforingsoversikt";
import { useNavTiltaksgjennomforinger } from "@/core/api/queries/useTiltaksgjennomforinger";
import { FilterMenyWithSkeletonLoader } from "../../../components/filtrering/FilterMenyWithSkeletonLoader";

export const NavArbeidsmarkedstiltakOversikt = () => {
  const { data: tiltaksgjennomforinger = [], isLoading } = useNavTiltaksgjennomforinger();

  return (
    <FilterAndTableLayout
      buttons={null}
      filter={<FilterMenyWithSkeletonLoader />}
      tags={<Filtertags />}
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
