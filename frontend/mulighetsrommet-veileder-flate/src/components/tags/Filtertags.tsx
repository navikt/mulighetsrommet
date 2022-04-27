import React from 'react';
import { Tag } from '@navikt/ds-react';
import { Close } from '@navikt/ds-icons';
import Ikonknapp from '../knapper/Ikonknapp';
import './Filtertags.less';

interface FilterTagsProps {
  options: any[];
  handleClick: (id: number) => void;
}

const FilterTags = ({ options, handleClick }: FilterTagsProps) => {
  return (
    <>
      {options.map(filtertype => (
        <Tag key={filtertype.id} variant="info" size="small">
          {filtertype.tittel}
          <Ikonknapp handleClick={() => handleClick(filtertype.id)} ariaLabel="Lukkeknapp">
            <Close className="filtertags__ikon" aria-label="Lukkeknapp" />
          </Ikonknapp>
        </Tag>
      ))}
    </>
  );
};

export default FilterTags;
