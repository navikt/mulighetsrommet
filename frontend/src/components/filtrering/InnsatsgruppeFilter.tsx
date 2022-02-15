import React from 'react';
import { Accordion, Checkbox, CheckboxGroup } from '@navikt/ds-react';
import { useInnsatsgrupper } from '../../hooks/tiltakstype/useInnsatsgrupper';

interface InnsatsgruppeFilterProps {
  innsatsgruppefilter: number[];
  setInnsatsgruppefilter: (innsatsgrupper: number[]) => void;
}
const InnsatsgruppeFilter = ({ innsatsgruppefilter, setInnsatsgruppefilter }: InnsatsgruppeFilterProps) => {
  const innsatsgrupper = useInnsatsgrupper().data;

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = parseInt(e.target.value);
    if (!innsatsgruppefilter?.includes(value)) {
      setInnsatsgruppefilter([...(innsatsgruppefilter ?? []), value]);
    } else {
      setInnsatsgruppefilter(innsatsgruppefilter.filter((elem: number) => elem !== value));
    }
  };

  return (
    <Accordion>
      <Accordion.Item defaultOpen>
        <Accordion.Header>Innsatsgruppe</Accordion.Header>
        <Accordion.Content>
          <CheckboxGroup legend="" hideLegend size="small">
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
        </Accordion.Content>
      </Accordion.Item>
    </Accordion>
  );
};

export default InnsatsgruppeFilter;
