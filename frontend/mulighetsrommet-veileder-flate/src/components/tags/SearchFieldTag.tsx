import React from 'react';
import { Tag } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { tiltakstypefilter } from '../../api/atoms/atoms';
import { Close } from '@navikt/ds-icons';
import Ikonknapp from '../knapper/Ikonknapp';
import './Filtertags.less';

const SearchFieldTag = () => {
  const [filter, setFilter] = useAtom(tiltakstypefilter);

  const handleClickFjernFilter = () => {
    setFilter({
      ...filter,
      search: '',
    });
  };

  return (
    <>
      {filter.search && (
        <Tag variant="info" size="small">
          Søk på tittel
          <Ikonknapp handleClick={handleClickFjernFilter} ariaLabel="Lukkeknapp">
            <Close className="filtertags__ikon" aria-label="Lukkeknapp" />
          </Ikonknapp>
        </Tag>
      )}
    </>
  );
};

export default SearchFieldTag;
