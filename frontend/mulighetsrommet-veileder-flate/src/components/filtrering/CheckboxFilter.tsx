import React from 'react';
import { Accordion, Alert, Checkbox, CheckboxGroup, Loader } from '@navikt/ds-react';
import './Filtermeny.less';
import { kebabCase } from '../../utils/Utils';

interface CheckboxFilterProps {
  accordionNavn: string;
  options: object[];
  setOptions: (type: any[]) => void;
  data: any[];
  isLoading: boolean;
  isError: boolean;
  defaultOpen?: boolean;
  sortert?: boolean;
}

const CheckboxFilter = ({
  accordionNavn,
  options,
  setOptions,
  data,
  isLoading,
  isError,
  defaultOpen = false,
  sortert = false,
}: CheckboxFilterProps) => {
  const valgteTypeIDer = options!.map((type: any) => type.id);

  const handleFjernFilter = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = parseInt(e.target.value);
    const valgteTyper = !valgteTypeIDer.includes(value)
      ? valgteTypeIDer.concat(value)
      : valgteTypeIDer.filter((id: number) => id !== value);
    setOptions(data?.filter(type => valgteTyper.includes(type.id)) ?? []);
  };

  const checkbox = (filtertype: any, index: number) => {
    return (
      <Checkbox
        key={index}
        value={filtertype.id.toString()}
        onChange={handleFjernFilter}
        data-testid={`filter_checkbox_${kebabCase(filtertype.tittel)}`}
      >
        {filtertype.tittel}
      </Checkbox>
    );
  };

  return (
    <Accordion>
      <Accordion.Item defaultOpen={defaultOpen}>
        <Accordion.Header data-testid={`filter_accordionheader_${kebabCase(accordionNavn)}`}>
          {accordionNavn}
        </Accordion.Header>
        <Accordion.Content data-testid={`filter_accordioncontent_${kebabCase(accordionNavn)}`}>
          {isLoading && <Loader className="filter-loader" size="xlarge" />}
          {data && (
            <CheckboxGroup legend="" hideLegend size="small" value={valgteTypeIDer.map(String)}>
              {sortert
                ? data
                    .sort(function (a: { tittel: number }, b: { tittel: number }) {
                      if (a.tittel < b.tittel) return -1;
                      else if (a.tittel > b.tittel) return 1;
                      else return 0;
                    })
                    .map((filtertype, index) => checkbox(filtertype, index))
                : data.map((filtertype, index) => checkbox(filtertype, index))}
            </CheckboxGroup>
          )}
          {isError && <Alert variant="error">Det har skjedd en feil</Alert>}
        </Accordion.Content>
      </Accordion.Item>
    </Accordion>
  );
};

export default CheckboxFilter;
