import { FilterAndTableLayout } from "../../components/filter/FilterAndTableLayout";
import { TiltaksgjennomforingFilter } from "../../components/filter/Tiltaksgjennomforingfilter";
import { gjennomforingerForAvtaleFilterAtomFamily } from "../../api/atoms";
import { TiltaksgjennomforingFiltertags } from "../../components/filter/TiltaksgjennomforingFiltertags";
import { TiltaksgjennomforingFilterButtons } from "../../components/filter/TiltaksgjennomforingFilterButtons";
import { TiltaksgjennomforingsTabell } from "../../components/tabell/TiltaksgjennomforingsTabell";
import { useGetAvtaleIdFromUrlOrThrow } from "../../hooks/useGetAvtaleIdFromUrl";
import { useState } from "react";

export function TiltaksgjennomforingerForAvtalePage() {
  const id = useGetAvtaleIdFromUrlOrThrow();

  const filterAtom = gjennomforingerForAvtaleFilterAtomFamily(id);
  const [filterOpen, setFilterOpen] = useState<boolean>(true);

  return (
    <div style={{ marginTop: "1rem" }}>
      <FilterAndTableLayout
        filter={
          <TiltaksgjennomforingFilter
            filterAtom={filterAtom}
            skjulFilter={{
              tiltakstype: true,
            }}
          />
        }
        tags={<TiltaksgjennomforingFiltertags filterAtom={filterAtom} filterOpen={filterOpen} />}
        buttons={<TiltaksgjennomforingFilterButtons filterAtom={filterAtom} />}
        table={
          <TiltaksgjennomforingsTabell
            skjulKolonner={{
              tiltakstype: true,
              arrangor: true,
            }}
            filterAtom={filterAtom}
          />
        }
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
      />
    </div>
  );
}
