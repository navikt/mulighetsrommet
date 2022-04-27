import React from 'react';
import { Accordion, Alert, Checkbox, CheckboxGroup, Loader } from '@navikt/ds-react';
import './Filtermeny.less';

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
    setOptions(data?.filter((type: any) => valgteTyper.includes(type.id)) ?? []);
  };

  const sortertListe = (data: any[]) => {
    return data
      .sort(function (a: { tittel: number }, b: { tittel: number }) {
        if (a.tittel < b.tittel) return -1;
        else if (a.tittel > b.tittel) return 1;
        else return 0;
      })
      .map((filtertype: any) => (
        <Checkbox key={filtertype.id} value={filtertype.id.toString()} onChange={handleFjernFilter}>
          {filtertype.tittel}
        </Checkbox>
      ));
  };

  return (
    <Accordion>
      <Accordion.Item defaultOpen={defaultOpen}>
        <Accordion.Header>{accordionNavn}</Accordion.Header>
        <Accordion.Content>
          {isLoading && <Loader className="filter-loader" size="xlarge" />}
          {data && (
            <CheckboxGroup legend="" hideLegend size="small" value={valgteTypeIDer.map(String)}>
              {sortert
                ? sortertListe(data)
                : data.map((filtertype: any) => (
                    <Checkbox key={filtertype.id} value={filtertype.id.toString()} onChange={handleFjernFilter}>
                      {filtertype.tittel}
                    </Checkbox>
                  ))}
            </CheckboxGroup>
          )}
          {isError && <Alert variant="error">Det har skjedd en feil...</Alert>}
        </Accordion.Content>
      </Accordion.Item>
    </Accordion>
  );
};

export default CheckboxFilter;
