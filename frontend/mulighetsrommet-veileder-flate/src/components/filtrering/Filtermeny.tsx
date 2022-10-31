import { usePrepopulerFilter } from '../../hooks/usePrepopulerFilter';
import { FilterForIndividueltEllerGruppetiltak } from './FilterForIndividueltEllerGruppetiltak';
import styles from './Filtermeny.module.scss';
import InnsatsgruppeFilter from './InnsatsgruppeFilter';
import { Tiltakstypefilter } from './Tiltakstypefilter';
import { LokasjonFilter } from './LokasjonFilter';
import { useAtom } from 'jotai';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import Sokefelt from './Sokefelt';

const Filtermeny = () => {
  usePrepopulerFilter();
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);

  return (
    <div className={styles.tiltakstype_oversikt_filtermeny}>
      <Sokefelt sokefilter={filter.search!} setSokefilter={(search: string) => setFilter({ ...filter, search })} />
      <InnsatsgruppeFilter />
      <Tiltakstypefilter />
      <FilterForIndividueltEllerGruppetiltak />
      <LokasjonFilter />
    </div>
  );
};

export default Filtermeny;
