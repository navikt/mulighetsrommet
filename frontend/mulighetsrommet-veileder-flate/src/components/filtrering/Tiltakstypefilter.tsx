import { useTiltakstyper } from "@/api/queries/useTiltakstyper";
import { useArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";
import CheckboxFilter from "./CheckboxFilter";
import { FilterAccordionHeader } from "@mr/frontend-common";

interface Props {
  antallValgteTiltakstyper: number;
}

export function Tiltakstypefilter({ antallValgteTiltakstyper }: Props) {
  const tiltakstyper = useTiltakstyper();
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();
  return (
    <CheckboxFilter
      accordionHeader={
        <FilterAccordionHeader tittel="Tiltakstype" antallValgteFilter={antallValgteTiltakstyper} />
      }
      accordionNavn="Tiltakstyper"
      options={filter.tiltakstyper}
      setOptions={(tiltakstyper) => setFilter({ ...filter, tiltakstyper })}
      data={
        tiltakstyper.data?.map((tiltakstype) => {
          return {
            id: tiltakstype.sanityId,
            tittel: tiltakstype.navn,
          };
        }) ?? []
      }
      isError={tiltakstyper.isError}
      sortert
    />
  );
}
