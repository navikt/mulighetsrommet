import { useTiltakstyper } from "../../core/api/queries/useTiltakstyper";
import CheckboxFilter from "./CheckboxFilter";
import { useArbeidsmarkedstiltakFilter } from "../../hooks/useArbeidsmarkedstiltakFilter";

export function Tiltakstypefilter() {
  const tiltakstyper = useTiltakstyper();
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();
  return (
    <CheckboxFilter
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
      isLoading={tiltakstyper.isLoading}
      isError={tiltakstyper.isError}
      sortert
    />
  );
}
