import { Filtermeny } from "@/components/filtrering/Filtermeny";
import { Tiltaksgjennomforingsoversikt } from "@/components/oversikt/Tiltaksgjennomforingsoversikt";
import { Feilmelding } from "@/components/feilmelding/Feilmelding";
import { FilterAndTableLayout } from "@/components/filtrering/FilterAndTableLayout";
import { usePreviewTiltaksgjennomforinger } from "@/core/api/queries/useTiltaksgjennomforinger";
import { Filtertags } from "@/components/filtrering/Filtertags";
import { TiltakLoader } from "@/components/TiltakLoader";

export const PreviewArbeidsmarkedstiltakOversikt = () => {
  const { data: tiltaksgjennomforinger = [], isLoading } = usePreviewTiltaksgjennomforinger();

  return (
    <FilterAndTableLayout
      buttons={null}
      filter={<Filtermeny />}
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
