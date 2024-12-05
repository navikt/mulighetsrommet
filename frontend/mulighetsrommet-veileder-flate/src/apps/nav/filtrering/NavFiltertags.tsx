import { ApentForPamelding } from "@mr/api-client";
import { useArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { NavEnhetFilterTag } from "@mr/frontend-common/components/filter/filterTag/NavEnhetFilterTag";
import { FilterTagsContainer } from "@mr/frontend-common/components/filter/filterTag/FilterTagsContainer";
import { FilterTag } from "@mr/frontend-common/components/filter/filterTag/FilterTag";

interface Props {
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function NavFiltertags({ filterOpen, setTagsHeight }: Props) {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      {filter.search && (
        <FilterTag label={filter.search} onClose={() => setFilter({ ...filter, search: "" })} />
      )}
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
      {filter.innsatsgruppe && (
        <FilterTag
          label={filter.innsatsgruppe.tittel}
          onClose={() => {
            setFilter({ ...filter, innsatsgruppe: undefined });
          }}
        />
      )}
      <NavEnhetFilterTag
        navEnheter={filter.navEnheter}
        onClose={() => setFilter({ ...filter, navEnheter: [] })}
      />
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
    </FilterTagsContainer>
  );
}
