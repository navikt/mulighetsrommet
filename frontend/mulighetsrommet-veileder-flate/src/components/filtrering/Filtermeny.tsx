import React from 'react';
import { Heading } from '@navikt/ds-react';
import InnsatsgruppeFilter from './InnsatsgruppeFilter';
import './Filtermeny.less';
import { useAtom } from 'jotai';
import Searchfield from './Searchfield';
import { tiltakstypefilter } from '../../core/atoms/atoms';

const Filtermeny = () => {
  const [filter, setFilter] = useAtom(tiltakstypefilter);

  return (
    <div className="tiltakstype-oversikt__filtermeny">
      <Heading size="medium" level="1" className="filtermeny__heading" role="heading">
        Filter
      </Heading>
      <Searchfield sokefilter={filter.search!} setSokefilter={(search: string) => setFilter({ ...filter, search })} />
      <InnsatsgruppeFilter
        innsatsgruppefilter={filter.innsatsgrupper!}
        setInnsatsgruppefilter={innsatsgrupper => setFilter({ ...filter, innsatsgrupper })}
      />
    </div>
  );
};

export default Filtermeny;
