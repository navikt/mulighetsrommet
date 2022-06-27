import React from 'react';
import { Tag } from '@navikt/ds-react';
import { Close } from '@navikt/ds-icons';
import Ikonknapp from '../knapper/Ikonknapp';
import './Filtertags.less';
import { kebabCase } from '../../utils/Utils';

interface FilterTagsProps {
  options: { id: string; tittel: string }[];
  handleClick: (id: string) => void;
}

const FilterTags = ({ options, handleClick }: FilterTagsProps) => {
  return (
    <>
      {options.map(filtertype => {
        return (
          <Tag
            key={filtertype.id}
            variant="info"
            size="small"
            data-testid={`filtertag_${kebabCase(filtertype.tittel)}`}
          >
            {filtertype.tittel}
            <Ikonknapp handleClick={() => handleClick(filtertype.id)} ariaLabel="Lukkeknapp">
              <Close className="filtertags__ikon" aria-label="Lukkeknapp" />
            </Ikonknapp>
          </Tag>
        );
      })}
    </>
  );
};

export default FilterTags;
