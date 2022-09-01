import { Close } from '@navikt/ds-icons';
import { Tag } from '@navikt/ds-react';
import { kebabCase } from '../../utils/Utils';
import Ikonknapp from '../knapper/Ikonknapp';
import './Filtertags.less';

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
            <Ikonknapp
              handleClick={() => handleClick(filtertype.id)}
              ariaLabel="Lukke"
              data-testid={`filtertag_lukkeknapp_${kebabCase(filtertype.tittel)}`}
            >
              <Close
                data-testid={`filtertag_lukkeknapp_${kebabCase(filtertype.tittel)}`}
                className="filtertags__ikon"
                aria-label="Lukke"
              />
            </Ikonknapp>
          </Tag>
        );
      })}
    </>
  );
};

export default FilterTags;
