import { useAtom } from 'jotai';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import Searchfield from './Searchfield';

export function Fritekstfilter() {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  return (
    <Searchfield sokefilter={filter.search!} setSokefilter={(search: string) => setFilter({ ...filter, search })} />
  );
}
