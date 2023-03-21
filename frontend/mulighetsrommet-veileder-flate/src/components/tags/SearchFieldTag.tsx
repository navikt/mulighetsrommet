import { Tag } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { RESET } from 'jotai/utils';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import Ikonknapp from '../knapper/Ikonknapp';
import style from './Filtertag.module.scss';
import { XMarkIcon } from '@navikt/aksel-icons';

const SearchFieldTag = () => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);

  const handleClickFjernFilter = () => {
    setFilter(RESET);
  };

  return (
    <>
      {filter.search && (
        <Tag className="cypress-tag" variant="info" size="small">
          {`'${filter.search}'`}
          <Ikonknapp
            handleClick={handleClickFjernFilter}
            ariaLabel="Lukkeknapp"
            icon={<XMarkIcon className={style.ikon} aria-label="Lukkeknapp" />}
          />
        </Tag>
      )}
    </>
  );
};

export default SearchFieldTag;
