import React from 'react';
import '../../views/tiltakstype-oversikt/TiltakstypeOversikt.less';
import { useAtom } from 'jotai';
import { tiltakstypeOversiktSok } from '../../api/atoms/atoms';
import { TextField } from '@navikt/ds-react';

const Sokefelt = () => {
  const [sok, setSok] = useAtom(tiltakstypeOversiktSok);
  return (
    <TextField
      label="Søk etter tiltakstype:"
      onChange={e => setSok(e.currentTarget.value)}
      value={sok}
      data-testid="sokefelt_tiltakstype"
      className="sokefelt-tiltakstype"
    />
  );
};

export default Sokefelt;
