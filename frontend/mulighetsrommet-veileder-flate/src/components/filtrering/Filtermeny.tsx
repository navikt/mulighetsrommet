import React from 'react';
import { Heading } from '@navikt/ds-react';
import InnsatsgruppeFilter from './InnsatsgruppeFilter';
import './Filtermeny.less';
import Ikonknapp from '../knapper/Ikonknapp';
import { Close } from '@navikt/ds-icons';
import { useAtom } from 'jotai';
import Searchfield from './Searchfield';
import { tiltakstypefilter } from '../../core/atoms/atoms';

interface SidemenyProps {
  handleClickSkjulSidemeny: () => void;
}

const Filtermeny = ({ handleClickSkjulSidemeny }: SidemenyProps) => {
  const [filter, setFilter] = useAtom(tiltakstypefilter);

  return (
    <div className="tiltakstype-oversikt__filtermeny">
      <Heading size="medium" level="1" className="sidemeny__heading" role="heading">
        Filter
        <Ikonknapp handleClick={handleClickSkjulSidemeny} ariaLabel="Lukkeknapp">
          <Close aria-label="Lukkeknapp" />
        </Ikonknapp>
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
