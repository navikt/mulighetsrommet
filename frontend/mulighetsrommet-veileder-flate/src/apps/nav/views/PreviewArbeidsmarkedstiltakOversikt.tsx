import { TiltakLoader } from "@/components/TiltakLoader";
import { Feilmelding } from "@/components/feilmelding/Feilmelding";
import { FilterAndTableLayout } from "@/components/filtrering/FilterAndTableLayout";
import { Filtertags } from "@/components/filtrering/Filtertags";
import { Tiltaksgjennomforingsoversikt } from "@/components/oversikt/Tiltaksgjennomforingsoversikt";
import { usePreviewTiltaksgjennomforinger } from "@/core/api/queries/useTiltaksgjennomforinger";
import { FilterMenyMedSkeletonLoader } from "@/components/filtrering/FilterMenyMedSkeletonLoader";

export const PreviewArbeidsmarkedstiltakOversikt = () => {
  const { data: tiltaksgjennomforinger = [], isLoading } = usePreviewTiltaksgjennomforinger();

  return (
    <FilterAndTableLayout
      buttons={null}
      filter={<FilterMenyMedSkeletonLoader />}
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
