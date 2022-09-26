import { useAtom } from 'jotai';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import CheckboxFilter from './CheckboxFilter';
import useLokasjonerForBruker from "../../core/api/queries/useLokasjonerForBruker";

export function LokasjonFilter() {
  const lokasjoner = useLokasjonerForBruker();
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  console.log(filter)
  console.log(lokasjoner)
  console.log(filter.lokasjoner)
  return (
    <CheckboxFilter
      accordionNavn="Lokasjon"
      options={filter.lokasjoner ?? []}
      setOptions={lokasjoner => setFilter({ ...filter, lokasjoner })}
      data={
        lokasjoner.data?.map(lokasjon => {
          return {
            id: lokasjon,
            tittel: lokasjon,
          };
        }) ?? []
      }
      isLoading={lokasjoner.isLoading}
      isError={lokasjoner.isError}
      sortert
      // defaultOpen={filter.lokasjoner?.length ?? > 0}
    />
  );
}
