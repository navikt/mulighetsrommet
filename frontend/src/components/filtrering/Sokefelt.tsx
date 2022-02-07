import React from 'react';
import '../../views/tiltaksvariant-oversikt/TiltaksvariantOversikt.less';
import { useAtom } from 'jotai';
import { tiltaksvariantOversiktSok } from '../../api/atoms/atoms';
import { TextField } from '@navikt/ds-react';

const Sokefelt = () => {
  const [sok, setSok] = useAtom(tiltaksvariantOversiktSok);
  return (
    <TextField
      label="Søk etter tiltaksvariant:"
      onChange={e => setSok(e.currentTarget.value)}
      value={sok}
      data-testid="sokefelt_tiltaksvariant"
      className="sokefelt-tiltaksvariant"
    />
  );
};

export default Sokefelt;
