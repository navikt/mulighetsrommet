import { ApentForInnsok } from "mulighetsrommet-api-client";
import {
  ArbeidsmarkedstiltakFilterGruppe,
  useArbeidsmarkedstiltakFilter,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { Filtertag } from "mulighetsrommet-frontend-common/components/filter/filtertag/Filtertag";
import { NavEnhetFiltertag } from "mulighetsrommet-frontend-common/components/filter/filtertag/NavEnhetFiltertag";
import { FiltertagsContainer } from "mulighetsrommet-frontend-common/components/filter/filtertag/FiltertagsContainer";

interface Props {
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function ModiaFiltertags({ filterOpen, setTagsHeight }: Props) {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();

  return (
    <FiltertagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      {filter.innsatsgruppe && <Filtertag label={filter.innsatsgruppe.tittel} />}
      <NavEnhetFiltertag
        navEnheter={filter.navEnheter}
        onClose={() => setFilter({ ...filter, navEnheter: [] })}
      />
      {filter.apentForInnsok !== ApentForInnsok.APENT_ELLER_STENGT && (
        <Filtertag
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
        <Filtertag
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
        <Filtertag label={filter.search} onClose={() => setFilter({ ...filter, search: "" })} />
      )}
    </FiltertagsContainer>
  );
}
