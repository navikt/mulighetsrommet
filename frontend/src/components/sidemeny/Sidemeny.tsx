import React from 'react';
import { Heading } from '@navikt/ds-react';
import InnsatsgruppeFilter from '../filtrering/InnsatsgruppeFilter';
import './Sidemeny.less';
import Sokefelt from '../filtrering/Sokefelt';
import Ikonknapp from '../knapper/Ikonknapp';
import { Close } from '@navikt/ds-icons';
import { useAtom } from 'jotai';
import { tiltakstypefilter } from '../../api/atoms/atoms';

interface SidemenyProps {
  handleClickSkjulSidemeny: () => void;
}

const Sidemeny = ({ handleClickSkjulSidemeny }: SidemenyProps) => {
  const [filter, setFilter] = useAtom(tiltakstypefilter);

  return (
    <div className="tiltakstype-oversikt__sidemeny">
      <Heading size="large" level="2" className="sidemeny__heading">
        Filter
        <Ikonknapp handleClick={handleClickSkjulSidemeny}>
          <Close />
        </Ikonknapp>
      </Heading>
      <Sokefelt sokefilter={filter.search ?? ''} setSokefilter={(search: string) => setFilter({ ...filter, search })} />
      <InnsatsgruppeFilter
        innsatsgruppefilter={filter.innsatsgrupper ?? []}
        setInnsatsgruppefilter={innsatsgrupper => setFilter({ ...filter, innsatsgrupper })}
      />
    </div>
  );
};

export default Sidemeny;
