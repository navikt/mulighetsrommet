import { useAtom } from "jotai";
import { useTiltakstyper } from "../../core/api/queries/useTiltakstyper";
import { tiltaksgjennomforingsfilter } from "../../core/atoms/atoms";
import CheckboxFilter from "./CheckboxFilter";

export function Tiltakstypefilter() {
  const tiltakstyper = useTiltakstyper();
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
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
