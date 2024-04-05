import { ApentForInnsok } from "mulighetsrommet-api-client";
import {
  ArbeidsmarkedstiltakFilterGruppe,
  useArbeidsmarkedstiltakFilter,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { FilterTag } from "mulighetsrommet-frontend-common/components/filter/filterTag/FilterTag";
import { NavEnhetFilterTag } from "mulighetsrommet-frontend-common/components/filter/filterTag/NavEnhetFilterTag";
import { FilterTagsContainer } from "mulighetsrommet-frontend-common/components/filter/filterTag/FilterTagsContainer";

interface Props {
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function ModiaFiltertags({ filterOpen, setTagsHeight }: Props) {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      {filter.innsatsgruppe && <FilterTag label={filter.innsatsgruppe.tittel} />}
      <NavEnhetFilterTag
        navEnheter={filter.navEnheter}
        onClose={() => setFilter({ ...filter, navEnheter: [] })}
      />
      {filter.apentForInnsok !== ApentForInnsok.APENT_ELLER_STENGT && (
        <FilterTag
          label={filter.apentForInnsok === ApentForInnsok.APENT ? "Åpent" : "Stengt"}
          onClose={() =>
            setFilter({
              ...filter,
              apentForInnsok: ApentForInnsok.APENT_ELLER_STENGT,
            })
          }
        />
      )}
      {filter.tiltakstyper.map((tiltakstype) => (
        <FilterTag
          key={tiltakstype.id}
          label={tiltakstype.tittel}
          onClose={() =>
            setFilter({
              ...filter,
              tiltakstyper: filter.tiltakstyper?.filter(
                (type: ArbeidsmarkedstiltakFilterGruppe<string>) => tiltakstype.id !== type.id,
              ),
            })
          }
        />
      ))}
      {filter.search && (
        <FilterTag label={filter.search} onClose={() => setFilter({ ...filter, search: "" })} />
      )}
    </FilterTagsContainer>
  );
}
