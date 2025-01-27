import { ApentForPamelding } from "@mr/api-client-v2";
import { useArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { FilterTag } from "@mr/frontend-common/components/filter/filterTag/FilterTag";
import { NavEnhetFilterTag } from "@mr/frontend-common/components/filter/filterTag/NavEnhetFilterTag";
import { FilterTagsContainer } from "@mr/frontend-common/components/filter/filterTag/FilterTagsContainer";

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
      {filter.apentForPamelding !== ApentForPamelding.APENT_ELLER_STENGT && (
        <FilterTag
          label={filter.apentForPamelding === ApentForPamelding.APENT ? "Åpent" : "Stengt"}
          onClose={() =>
            setFilter({
              ...filter,
              apentForPamelding: ApentForPamelding.APENT_ELLER_STENGT,
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
              tiltakstyper: filter.tiltakstyper?.filter(({ id }) => tiltakstype.id !== id),
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
