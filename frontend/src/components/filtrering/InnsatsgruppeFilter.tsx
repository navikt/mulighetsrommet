import React from 'react';
import { Checkbox, CheckboxGroup } from '@navikt/ds-react';
import { useInnsatsgrupper } from '../../hooks/tiltaksvariant/useInnsatsgrupper';
import { useAtom } from 'jotai';
import { filtreringInnsatsgruppe } from '../../api/atoms/atoms';

const InnsatsgruppeFilter = () => {
  const innsatsgrupper = useInnsatsgrupper().data;

  const [valgteInnsatsgrupper, setValgteInnsatsgrupper] = useAtom(filtreringInnsatsgruppe);

  const handleChange = (e: any) => {
    const value = parseInt(e.target.value);
    if (!valgteInnsatsgrupper.includes(value)) {
      setValgteInnsatsgrupper([...valgteInnsatsgrupper, value]);
    } else {
      setValgteInnsatsgrupper(valgteInnsatsgrupper.filter(elem => elem !== value));
    }
  };

  return (
    <CheckboxGroup legend="Innsatsgruppe" size="small">
      {innsatsgrupper?.map(innsatsgruppe => (
        <Checkbox
          key={innsatsgruppe.id}
          value={innsatsgruppe.id.toString()}
          name={innsatsgruppe.tittel}
          onChange={handleChange}
        >
          {innsatsgruppe.tittel}
        </Checkbox>
      ))}
    </CheckboxGroup>
  );
};

export default InnsatsgruppeFilter;
