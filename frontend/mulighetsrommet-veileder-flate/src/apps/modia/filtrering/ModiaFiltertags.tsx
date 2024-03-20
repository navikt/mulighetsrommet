import { ApentForInnsok } from "mulighetsrommet-api-client";
import {
  ArbeidsmarkedstiltakFilterGruppe,
  useArbeidsmarkedstiltakFilter,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { Filtertag } from "mulighetsrommet-frontend-common/components/filter/filtertag/Filtertag";
import { NavEnhetFiltertag } from "mulighetsrommet-frontend-common/components/filter/filtertag/NavEnhetFiltertag";
import { FiltertagsContainer } from "mulighetsrommet-frontend-common/components/filter/filtertag/FiltertagsContainer";

interface Props {
  filterOpen?: boolean;
}

export function ModiaFiltertags({ filterOpen }: Props) {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();

  return (
    <FiltertagsContainer filterOpen={filterOpen}>
      {filter.innsatsgruppe && <Filtertag options={[filter.innsatsgruppe]} />}
      <NavEnhetFiltertag onClose={() => setFilter({ ...filter, regionMap: {} })} />
      {filter.apentForInnsok !== ApentForInnsok.APENT_ELLER_STENGT && (
        <Filtertag
          options={[
            {
              id: filter.apentForInnsok,
              tittel: filter.apentForInnsok === ApentForInnsok.APENT ? "Åpent" : "Stengt",
            },
          ]}
          onClose={() =>
            setFilter({
              ...filter,
              apentForInnsok: ApentForInnsok.APENT_ELLER_STENGT,
            })
          }
        />
      )}
      <Filtertag
        options={filter.tiltakstyper}
        onClose={(id: string) =>
          setFilter({
            ...filter,
            tiltakstyper: filter.tiltakstyper?.filter(
              (tiltakstype: ArbeidsmarkedstiltakFilterGruppe<string>) => tiltakstype.id !== id,
            ),
          })
        }
      />
      {filter.search && (
        <Filtertag
          options={[{ id: "search", tittel: `'${filter.search}'` }]}
          onClose={() => setFilter({ ...filter, search: "" })}
        />
      )}
    </FiltertagsContainer>
  );
}
