import { useAtom } from 'jotai';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { usePrepopulerFilter } from '../../hooks/usePrepopulerFilter';
import styles from './Filtermeny.module.scss';
import InnsatsgruppeFilter from './InnsatsgruppeFilter';
import { LokasjonFilter } from './LokasjonFilter';
import Sokefelt from './Sokefelt';
import { Tiltakstypefilter } from './Tiltakstypefilter';
import { Accordion } from '@navikt/ds-react';

const Filtermeny = () => {
  usePrepopulerFilter();
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);

  return (
    <div className={styles.tiltakstype_oversikt_filtermeny} id="tiltakstype_oversikt_filtermeny">
      <Sokefelt sokefilter={filter.search} setSokefilter={(search: string) => setFilter({ ...filter, search })} />
      <Accordion>
        <InnsatsgruppeFilter />
        <Tiltakstypefilter />
        <LokasjonFilter />
      </Accordion>
    </div>
  );
};

export default Filtermeny;
