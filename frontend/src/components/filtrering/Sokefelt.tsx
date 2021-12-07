import React from 'react';
import { Input } from 'nav-frontend-skjema';
import '../../views/tiltaksvariant-oversikt/TiltaksvariantOversikt.less';
import { useAtom } from 'jotai';
import { tiltaksvariantOversiktSok } from '../../core/atoms/atoms';

const Sokefelt = () => {
  const [sok, setSok] = useAtom(tiltaksvariantOversiktSok);
  return (
    <Input
      label="Søk etter tiltaksvariant:"
      onChange={e => setSok(e.currentTarget.value)}
      value={sok}
      data-testid="sokefelt_tiltaksvariant"
      className="sokefelt-tiltaksvariant"
    />
  );
};

export default Sokefelt;
