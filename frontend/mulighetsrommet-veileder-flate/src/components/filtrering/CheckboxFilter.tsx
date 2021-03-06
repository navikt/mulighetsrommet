import React from 'react';
import { Accordion, Alert, Checkbox, CheckboxGroup, Loader } from '@navikt/ds-react';
import './Filtermeny.less';
import { kebabCase } from '../../utils/Utils';
import { logEvent } from '../../api/logger';

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

  const handleEndreFilter = (filtertypeTittel: string, e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    const valgteTyper = !valgteTypeIDer.includes(value)
      ? valgteTypeIDer.concat(value)
      : valgteTypeIDer.filter((id: string) => id !== value);
    setOptions(data?.filter(type => valgteTyper.includes(type.id)) ?? []);
    logEvent(`mulighetsrommet.filtrering.${accordionNavn.toLowerCase()}`, { filtertypeTittel });
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
    <Accordion role="menu">
      <Accordion.Item defaultOpen={defaultOpen}>
        <Accordion.Header data-testid={`filter_accordionheader_${kebabCase(accordionNavn)}`}>
          {accordionNavn}
        </Accordion.Header>
        <Accordion.Content role="menuitem" data-testid={`filter_accordioncontent_${kebabCase(accordionNavn)}`}>
          {isLoading && <Loader className="filter-loader" size="xlarge" />}
          {data && (
            <CheckboxGroup legend="" hideLegend size="small" value={valgteTypeIDer.map(String)}>
              {sortert ? data.sort((a, b) => a.tittel.localeCompare(b.tittel)).map(checkbox) : data.map(checkbox)}
            </CheckboxGroup>
          )}
          {isError && <Alert variant="error">Det har skjedd en feil</Alert>}
        </Accordion.Content>
      </Accordion.Item>
    </Accordion>
  );
};

export default CheckboxFilter;
