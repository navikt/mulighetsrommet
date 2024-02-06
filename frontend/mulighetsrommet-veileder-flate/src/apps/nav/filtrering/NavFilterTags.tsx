import { ApentForInnsok } from "mulighetsrommet-api-client";
import {
  ArbeidsmarkedstiltakFilterGruppe,
  useArbeidsmarkedstiltakFilter,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import FilterTag from "../../../components/tags/FilterTag";
import { NavEnhetTag } from "@/components/tags/NavEnhetTag";
import { FilterTagsContainer } from "@/components/filtrering/FilterTagsContainer";

export function NavFilterTags() {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();

  return (
    <FilterTagsContainer>
      {filter.search && (
        <FilterTag
          options={[{ id: "search", tittel: `'${filter.search}'` }]}
          onClose={() => setFilter({ ...filter, search: "" })}
        />
      )}
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
      {filter.innsatsgruppe && (
        <FilterTag
          options={[filter.innsatsgruppe]}
          onClose={() => {
            setFilter({ ...filter, innsatsgruppe: undefined });
          }}
        />
      )}
      <NavEnhetTag onClose={() => setFilter({ ...filter, regionMap: {} })} />
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
    </FilterTagsContainer>
  );
}
