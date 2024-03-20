import { ApentForInnsok } from "mulighetsrommet-api-client";
import {
  ArbeidsmarkedstiltakFilterGruppe,
  useArbeidsmarkedstiltakFilter,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { NavEnhetFiltertag } from "../../../../../frontend-common/components/filtertag/NavEnhetFiltertag";
import { FiltertagsContainer } from "../../../../../frontend-common/components/filtertag/FiltertagsContainer";
import { Filtertag } from "../../../../../frontend-common/components/filtertag/Filtertag";

interface Props {
  filterOpen?: boolean;
}

export function NavFiltertags({ filterOpen }: Props) {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();

  return (
    <FiltertagsContainer filterOpen={filterOpen}>
      {filter.search && (
        <Filtertag
          options={[{ id: "search", tittel: `'${filter.search}'` }]}
          onClose={() => setFilter({ ...filter, search: "" })}
        />
      )}
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
      {filter.innsatsgruppe && (
        <Filtertag
          options={[filter.innsatsgruppe]}
          onClose={() => {
            setFilter({ ...filter, innsatsgruppe: undefined });
          }}
        />
      )}
      <NavEnhetFiltertag onClose={() => setFilter({ ...filter, regionMap: {} })} />
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
    </FiltertagsContainer>
  );
}
