import { Button } from '@navikt/ds-react';
import { Filter } from '@navikt/ds-icons';
import React from 'react';
import './Filterknapp.less';
import { useAtom } from 'jotai';
import { visSidemeny } from '../../api/atoms/atoms';

const FilterKnapp = () => {
  const [sidemenyVisning, setSidemenyVisning] = useAtom(visSidemeny);

  return (
    <Button
      variant="tertiary"
      className="filterknapp"
      size="small"
      onClick={() => setSidemenyVisning(!sidemenyVisning)}
    >
      <Filter />
    </Button>
  );
};

export default FilterKnapp;
