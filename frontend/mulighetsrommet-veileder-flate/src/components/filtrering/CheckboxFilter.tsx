import React from 'react';
import { Accordion, Alert, Checkbox, CheckboxGroup, Loader } from '@navikt/ds-react';
import { kebabCase } from '../../utils/Utils';
import { logEvent } from '../../core/api/logger';

interface CheckboxFilterProps<T extends { id: string; tittel: string }> {
  accordionNavn: string;
  options: T[];
  setOptions: (type: T[]) => void;
  data: T[];
  isLoading: boolean;
  isError: boolean;
  defaultOpen?: boolean;
  sortert?: boolean;
}

const CheckboxFilter = <T extends { id: string; tittel: string }>({
  accordionNavn,
  options,
  setOptions,
  data,
  isLoading,
  isError,
  defaultOpen = false,
  sortert = false,
}: CheckboxFilterProps<T>) => {
  const valgteTypeIDer = options.map(type => type.id);
  const kebabCaseAccordionNavn = kebabCase(accordionNavn);

  const handleEndreFilter = (filtertypeTittel: string, e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    const valgteTyper = !valgteTypeIDer.includes(value)
      ? valgteTypeIDer.concat(value)
      : valgteTypeIDer.filter((id: string) => id !== value);
    setOptions(data?.filter(type => valgteTyper.includes(type.id)) ?? []);

    logEvent(`mulighetsrommet.filtrering`, {
      type: kebabCase(accordionNavn),
      value: kebabCase(filtertypeTittel),
    });
  };

  const checkbox = (filtertype: T) => {
    return (
      <Checkbox
        key={`${filtertype.id}`}
        value={filtertype.id}
        onChange={e => handleEndreFilter(filtertype.tittel, e)}
        data-testid={`filter_checkbox_${kebabCase(filtertype.tittel)}`}
      >
        {filtertype.tittel}
      </Checkbox>
    );
  };

  return (
    <Accordion.Item defaultOpen={defaultOpen}>
      <Accordion.Header data-testid={`filter_accordionheader_${kebabCaseAccordionNavn}`}>
        {accordionNavn}
      </Accordion.Header>
      <Accordion.Content data-testid={`filter_accordioncontent_${kebabCaseAccordionNavn}`}>
        {isLoading && !data ? <Loader size="xlarge" /> : null}
        {data && (
          <CheckboxGroup
            legend=""
            hideLegend
            size="small"
            value={valgteTypeIDer.map(String)}
            data-testid={`checkboxgroup_${kebabCaseAccordionNavn}`}
          >
            {sortert ? data.sort((a, b) => a.tittel.localeCompare(b.tittel)).map(checkbox) : data.map(checkbox)}
          </CheckboxGroup>
        )}
        {isError && <Alert variant="error">Det har skjedd en feil</Alert>}
      </Accordion.Content>
    </Accordion.Item>
  );
};

export default CheckboxFilter;
