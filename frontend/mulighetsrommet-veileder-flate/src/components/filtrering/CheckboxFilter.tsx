import { Accordion, Alert, Checkbox, CheckboxGroup, Loader } from "@navikt/ds-react";
import React from "react";
import { addOrRemove, kebabCase } from "../../utils/Utils";
import { filterAccordionAtom } from "../../core/atoms/atoms";
import { useAtom } from "jotai";

interface CheckboxFilterProps<T extends { id: string; tittel: string }> {
  accordionNavn: string;
  options: T[];
  setOptions: (type: T[]) => void;
  data: T[];
  isLoading: boolean;
  isError: boolean;
  sortert?: boolean;
}

const CheckboxFilter = <T extends { id: string; tittel: string }>({
  accordionNavn,
  options,
  setOptions,
  data,
  isLoading,
  isError,
  sortert = false,
}: CheckboxFilterProps<T>) => {
  const [accordionsOpen, setAccordionsOpen] = useAtom(filterAccordionAtom);
  const valgteTypeIDer = options.map((type) => type.id);
  const kebabCaseAccordionNavn = kebabCase(accordionNavn);

  const handleEndreFilter = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    const valgteTyper = !valgteTypeIDer.includes(value)
      ? valgteTypeIDer.concat(value)
      : valgteTypeIDer.filter((id: string) => id !== value);
    setOptions(data?.filter((type) => valgteTyper.includes(type.id)) ?? []);
  };

  const checkbox = (filtertype: T) => {
    return (
      <Checkbox
        key={`${filtertype.id}`}
        value={filtertype.id}
        onChange={(e) => handleEndreFilter(e)}
      >
        {filtertype.tittel}
      </Checkbox>
    );
  };

  return (
    <Accordion.Item open={accordionsOpen.includes(kebabCaseAccordionNavn)}>
      <Accordion.Header
        onClick={() => {
          setAccordionsOpen([...addOrRemove(accordionsOpen, kebabCaseAccordionNavn)]);
        }}
        data-testid={`filter_accordionheader_${kebabCaseAccordionNavn}`}
      >
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
            {sortert
              ? data.sort((a, b) => a.tittel.localeCompare(b.tittel)).map(checkbox)
              : data.map(checkbox)}
          </CheckboxGroup>
        )}
        {isError && <Alert variant="error">Det har skjedd en feil</Alert>}
      </Accordion.Content>
    </Accordion.Item>
  );
};

export default CheckboxFilter;
