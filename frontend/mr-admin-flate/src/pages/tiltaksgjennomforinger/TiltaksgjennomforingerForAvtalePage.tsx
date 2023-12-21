import { FilterAndTableLayout } from "../../components/filter/FilterAndTableLayout";
import { TiltaksgjennomforingFilter } from "../../components/filter/Tiltaksgjennomforingfilter";
import { gjennomforingerForAvtaleFilterAtomFamily } from "../../api/atoms";
import { TiltaksgjennomforingFilterTags } from "../../components/filter/TiltaksgjennomforingFilterTags";
import { TiltaksgjennomforingFilterButtons } from "../../components/filter/TiltaksgjennomforingFilterButtons";
import { TiltaksgjennomforingsTabell } from "../../components/tabell/TiltaksgjennomforingsTabell";
import { useGetAvtaleIdFromUrlOrThrow } from "../../hooks/useGetAvtaleIdFromUrl";

export function TiltaksgjennomforingerForAvtalePage() {
  const id = useGetAvtaleIdFromUrlOrThrow();

  const filterAtom = gjennomforingerForAvtaleFilterAtomFamily(id);

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
        tags={<TiltaksgjennomforingFilterTags filterAtom={filterAtom} />}
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
      />
    </div>
  );
}
