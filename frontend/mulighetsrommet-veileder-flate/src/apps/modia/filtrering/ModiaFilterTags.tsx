import { ApentForPamelding } from "@api-client";
import { useArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { FilterTagsContainer } from "@mr/frontend-common/components/filter/filterTag/FilterTagsContainer";
import { NavEnhetFilterTag } from "@/components/filtrering/NavEnhetFilterTag";
import { Chips } from "@navikt/ds-react";

interface Props {
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function ModiaFilterTags({ filterOpen, setTagsHeight }: Props) {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      <Chips>
        {filter.search && (
          <Chips.Removable onClick={() => setFilter({ ...filter, search: "" })}>
            {filter.search}
          </Chips.Removable>
        )}
        {filter.apentForPamelding !== ApentForPamelding.APENT_ELLER_STENGT && (
          <Chips.Removable
            onClick={() =>
              setFilter({
                ...filter,
                apentForPamelding: ApentForPamelding.APENT_ELLER_STENGT,
              })
            }
          >
            {filter.apentForPamelding === ApentForPamelding.APENT ? "Åpent" : "Stengt"}
          </Chips.Removable>
        )}
        {filter.innsatsgruppe && (
          <Chips.Removable
            onClick={() => {
              setFilter({ ...filter, innsatsgruppe: undefined });
            }}
            data-testid={`filtertag_${filter.innsatsgruppe.nokkel}`}
          >
            {filter.innsatsgruppe.tittel}
          </Chips.Removable>
        )}
        {filter.navEnheter.length > 0 && (
          <NavEnhetFilterTag
            navEnheter={filter.navEnheter}
            onClose={() => setFilter({ ...filter, navEnheter: [] })}
          />
        )}
        {filter.tiltakstyper.map((tiltakstype) => (
          <Chips.Removable
            key={tiltakstype.id}
            onClick={() =>
              setFilter({
                ...filter,
                tiltakstyper: filter.tiltakstyper.filter(({ id }) => tiltakstype.id !== id),
              })
            }
          >
            {tiltakstype.tittel}
          </Chips.Removable>
        ))}
      </Chips>
    </FilterTagsContainer>
  );
}
