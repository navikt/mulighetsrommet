import { Close } from '@navikt/ds-icons';
import { Tag } from '@navikt/ds-react';
import { kebabCase } from '../../utils/Utils';
import Ikonknapp from '../knapper/Ikonknapp';
import styles from './Filtertags.module.scss';

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
            className="cypress-tag"
            key={filtertype.id}
            variant="info"
            size="small"
            data-testid={`filtertag_${kebabCase(filtertype.tittel)}`}
          >
            {filtertype.tittel}
            <Ikonknapp
              className={styles.overstyrtIkonknapp}
              handleClick={() => handleClick(filtertype.id)}
              ariaLabel="Lukke"
              data-testid={`filtertag_lukkeknapp_${kebabCase(filtertype.tittel)}`}
              icon={
                <Close
                  data-testid={`filtertag_lukkeknapp_${kebabCase(filtertype.tittel)}`}
                  className={styles.ikon}
                  aria-label="Lukke"
                />
              }
            />
          </Tag>
        );
      })}
    </>
  );
};

export default FilterTags;
