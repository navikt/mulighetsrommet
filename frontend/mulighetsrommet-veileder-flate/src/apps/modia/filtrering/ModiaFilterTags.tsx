import { ApentForInnsok } from "mulighetsrommet-api-client";
import {
  ArbeidsmarkedstiltakFilterGruppe,
  useArbeidsmarkedstiltakFilter,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { FilterTag } from "../../../../../frontend-common/components/filtertag/Filtertag";
import { NavEnhetTag } from "../../../../../frontend-common/components/filtertag/NavEnhetTag";
import { FilterTagsContainer } from "../../../../../frontend-common/components/filtertag/FilterTagsContainer";

export function ModiaFilterTags() {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();

  return (
    <FilterTagsContainer>
      {filter.innsatsgruppe && <FilterTag options={[filter.innsatsgruppe]} />}
      <NavEnhetTag onClose={() => setFilter({ ...filter, regionMap: {} })} />
      {filter.apentForInnsok !== ApentForInnsok.APENT_ELLER_STENGT && (
        <FilterTag
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
      <FilterTag
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
        <FilterTag
          options={[{ id: "search", tittel: `'${filter.search}'` }]}
          onClose={() => setFilter({ ...filter, search: "" })}
        />
      )}
    </FilterTagsContainer>
  );
}
