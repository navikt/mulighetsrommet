import React from 'react';
import { Accordion, Alert, Loader, Radio, RadioGroup } from '@navikt/ds-react';
import './Filtermeny.less';
import { kebabCase } from '../../utils/Utils';
import { InnsatsgruppeNokler } from '../../core/api/models';

interface InnsatsgruppeFilterProps {
  accordionNavn: string;
  option: InnsatsgruppeNokler;
  setOption: (type: InnsatsgruppeNokler) => void;
  data: string[];
  isLoading: boolean;
  isError: boolean;
  defaultOpen?: boolean;
}

const InnsatsgruppeFilter = ({
  accordionNavn,
  option,
  setOption,
  data,
  isLoading,
  isError,
  defaultOpen = false,
}: InnsatsgruppeFilterProps) => {
  const radiobox = (filtertype: InnsatsgruppeNokler) => {
    return (
      <Radio value={filtertype} key={`${filtertype}`} data-testid={`filter_checkbox_${kebabCase(filtertype)}`}>
        {filtertype}
      </Radio>
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
            <RadioGroup
              legend=""
              hideLegend
              size="small"
              onChange={(e: InnsatsgruppeNokler) => {
                setOption(e);
              }}
              value={option}
            >
              {data.map(radiobox)}
            </RadioGroup>
          )}
          {isError && <Alert variant="error">Det har skjedd en feil</Alert>}
        </Accordion.Content>
      </Accordion.Item>
    </Accordion>
  );
};

export default InnsatsgruppeFilter;
