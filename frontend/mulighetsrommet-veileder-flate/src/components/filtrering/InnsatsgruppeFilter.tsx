import React from 'react';
import { Accordion, Alert, Checkbox, CheckboxGroup, Loader } from '@navikt/ds-react';
import { useInnsatsgrupper } from '../../hooks/tiltakstype/useInnsatsgrupper';
import { Innsatsgruppe } from "../../../../mulighetsrommet-api";

interface InnsatsgruppeFilterProps {
  innsatsgruppefilter: Innsatsgruppe[];
  setInnsatsgruppefilter: (innsatsgrupper: Innsatsgruppe[]) => void;
}

const InnsatsgruppeFilter = ({ innsatsgruppefilter, setInnsatsgruppefilter }: InnsatsgruppeFilterProps) => {
  const innsatsgrupper = useInnsatsgrupper().data;
  const innsatsgrupperLoading = useInnsatsgrupper().isLoading;
  const innsatsgrupperError = useInnsatsgrupper().isError;

  const valgteInnsatsgruppeIDer = innsatsgruppefilter.map(gruppe => gruppe.id);

  const handleFjernFilter = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = parseInt(e.target.value);
    const valgteInnsatsgrupper = !valgteInnsatsgruppeIDer.includes(value)
      ? valgteInnsatsgruppeIDer.concat(value)
      : valgteInnsatsgruppeIDer.filter(id => id !== value);
    setInnsatsgruppefilter(
      innsatsgrupper?.filter(innsatsgruppe => valgteInnsatsgrupper.includes(innsatsgruppe.id)) ?? []
    );
  };

  return (
    <Accordion>
      <Accordion.Item defaultOpen>
        <Accordion.Header>Innsatsgruppe</Accordion.Header>
        <Accordion.Content>
          {innsatsgrupperLoading && <Loader className={'filter-loader'} size="xlarge" />}
          {innsatsgrupper && (
            //TODO har bedt om endring fra designsystemet for value
            <CheckboxGroup legend="" hideLegend size="small" value={valgteInnsatsgruppeIDer.map(String)}>
              {innsatsgrupper?.map(innsatsgruppe => (
                <Checkbox key={innsatsgruppe.id} value={innsatsgruppe.id.toString()} onChange={handleFjernFilter}>
                  {innsatsgruppe.tittel}
                </Checkbox>
              ))}
            </CheckboxGroup>
          )}
          {innsatsgrupperError && <Alert variant="error">Det har skjedd en feil...</Alert>}
        </Accordion.Content>
      </Accordion.Item>
    </Accordion>
  );
};

export default InnsatsgruppeFilter;
