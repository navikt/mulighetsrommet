import React from 'react';
import { Checkbox, CheckboxGroup } from '@navikt/ds-react';
import { useInnsatsgrupper } from '../../../hooks/tiltaksvariant/useInnsatsgrupper';

const InnsatsgruppeFilter = () => {
  const innsatsgrupper = useInnsatsgrupper().data;
  return (
    <CheckboxGroup legend="Innsatsgruppe">
      {innsatsgrupper?.map(innsatsgruppe => (
        <Checkbox key={innsatsgruppe.id} value={innsatsgruppe.tittel}>
          {innsatsgruppe.tittel}
        </Checkbox>
      ))}
    </CheckboxGroup>
  );
};

export default InnsatsgruppeFilter;
