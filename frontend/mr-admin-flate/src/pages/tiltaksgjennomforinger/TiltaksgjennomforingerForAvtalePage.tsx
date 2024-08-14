import { FilterAndTableLayout } from "mulighetsrommet-frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TiltaksgjennomforingFilter } from "@/components/filter/TiltaksgjennomforingFilter";
import {
  gjennomforingerForAvtaleFilterAtomFamily,
  tiltaksgjennomforingfilterAtom,
  TiltaksgjennomforingFilterSchema,
} from "@/api/atoms";
import { TiltaksgjennomforingFiltertags } from "@/components/filter/TiltaksgjennomforingFiltertags";
import { TiltaksgjennomforingFilterButtons } from "@/components/filter/TiltaksgjennomforingFilterButtons";
import { TiltaksgjennomforingsTabell } from "@/components/tabell/TiltaksgjennomforingsTabell";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useState } from "react";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { NullstillKnappForTiltaksgjennomforinger } from "@/pages/tiltaksgjennomforinger/NullstillKnappForTiltaksgjennomforinger";
import { TilToppenKnapp } from "mulighetsrommet-frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { LagredeFilterOversikt } from "mulighetsrommet-frontend-common";
import { LagretDokumenttype } from "@mr/api-client";
import { useAtom } from "jotai/index";

export function TiltaksgjennomforingerForAvtalePage() {
  const id = useGetAvtaleIdFromUrlOrThrow();

  const filterAtomTiltaksgjennomforinger = gjennomforingerForAvtaleFilterAtomFamily(id);
  const [filterOpen, setFilterOpen] = useState<boolean>(true);
  const { data: avtale } = useAvtale();
  const [tagsHeight, setTagsHeight] = useState(0);
  const [filter, setFilter] = useAtom(tiltaksgjennomforingfilterAtom);

  return (
    <>
      <FilterAndTableLayout
        filter={
          <TiltaksgjennomforingFilter
            filterAtom={filterAtomTiltaksgjennomforinger}
            skjulFilter={{
              tiltakstype: true,
            }}
          />
        }
        lagredeFilter={
          <LagredeFilterOversikt
            setFilter={setFilter}
            filter={filter}
            dokumenttype={LagretDokumenttype.AVTALE}
            validateFilterStructure={(filter) => {
              return TiltaksgjennomforingFilterSchema.safeParse(filter).success;
            }}
          />
        }
        tags={
          <TiltaksgjennomforingFiltertags
            filterAtom={filterAtomTiltaksgjennomforinger}
            filterOpen={filterOpen}
            setTagsHeight={setTagsHeight}
          />
        }
        buttons={<TiltaksgjennomforingFilterButtons />}
        table={
          <TiltaksgjennomforingsTabell
            skjulKolonner={{
              tiltakstype: true,
              arrangor: true,
            }}
            filterAtom={filterAtomTiltaksgjennomforinger}
            tagsHeight={tagsHeight}
            filterOpen={filterOpen}
          />
        }
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
        nullstillFilterButton={
          <NullstillKnappForTiltaksgjennomforinger
            avtale={avtale}
            filterAtom={filterAtomTiltaksgjennomforinger}
          />
        }
      />
      <TilToppenKnapp />
    </>
  );
}
