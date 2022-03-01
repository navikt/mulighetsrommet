import React from 'react';
import { Tag } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { tiltakstypefilter } from '../../api/atoms/atoms';
import { Close } from '@navikt/ds-icons';
import Ikonknapp from '../knapper/Ikonknapp';
import './Filtertags.less';

const InnsatsgruppefilterTags = () => {
  const [filter, setFilter] = useAtom(tiltakstypefilter);

  const handleClickFjernFilter = (id: number) => {
    setFilter({
      ...filter,
      innsatsgrupper: filter.innsatsgrupper?.filter(innsatsgruppe => innsatsgruppe.id !== id),
    });
  };

  return (
    <>
      {filter.innsatsgrupper?.map(innsatsgruppe => (
        <Tag key={innsatsgruppe.id} variant="info" size="small">
          {innsatsgruppe.tittel}
          <Ikonknapp handleClick={() => handleClickFjernFilter(innsatsgruppe.id)} ariaLabel="Lukkeknapp">
            <Close className="filtertags__ikon" aria-label="Lukkeknapp" />
          </Ikonknapp>
        </Tag>
      ))}
    </>
  );
};

export default InnsatsgruppefilterTags;
