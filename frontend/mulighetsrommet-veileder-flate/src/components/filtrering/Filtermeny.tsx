import React from 'react';
import { Heading } from '@navikt/ds-react';
import './Filtermeny.less';
import { useAtom } from 'jotai';
import Searchfield from './Searchfield';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import CheckboxFilter from './CheckboxFilter';
import useTiltakstyper from '../../hooks/tiltakstype/useTiltakstyper';
import { useInnsatsgrupper } from '../../hooks/tiltakstype/useInnsatsgrupper';

const Filtermeny = () => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const innsatsgrupper = useInnsatsgrupper();
  const tiltakstyper = useTiltakstyper();

  return (
    <div className="tiltakstype-oversikt__filtermeny">
      <Heading size="medium" level="1" className="filtermeny__heading" role="heading">
        Filter
      </Heading>
      <Searchfield sokefilter={filter.search!} setSokefilter={(search: string) => setFilter({ ...filter, search })} />
      <CheckboxFilter
        accordionNavn="Innsatsgrupper"
        options={filter.innsatsgrupper!}
        setOptions={innsatsgrupper => setFilter({ ...filter, innsatsgrupper })}
        data={innsatsgrupper.data!}
        isLoading={innsatsgrupper.isLoading}
        isError={innsatsgrupper.isError}
        defaultOpen
      />
      <CheckboxFilter
        accordionNavn="Tiltakstyper"
        options={filter.tiltakstyper!}
        setOptions={tiltakstyper => setFilter({ ...filter, tiltakstyper })}
        data={
          tiltakstyper.data?.map(tiltakstype => {
            return {
              id: tiltakstype.id,
              tittel: tiltakstype.navn,
            };
          }) ?? []
        }
        isLoading={tiltakstyper.isLoading}
        isError={tiltakstyper.isError}
        sortert
      />
    </div>
  );
};

export default Filtermeny;
