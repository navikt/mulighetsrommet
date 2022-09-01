import { Accordion, Alert, Loader, Radio, RadioGroup } from '@navikt/ds-react';
import { InnsatsgruppeNokler } from '../../core/api/models';
import { kebabCase } from '../../utils/Utils';
import './Filtermeny.less';

interface InnsatsgruppeFilterProps<T extends { id: string; tittel: string; nokkel?: InnsatsgruppeNokler }> {
  accordionNavn: string;
  option?: InnsatsgruppeNokler;
  setOption: (type: InnsatsgruppeNokler) => void;
  data: T[];
  isLoading: boolean;
  isError: boolean;
  defaultOpen?: boolean;
}

const InnsatsgruppeFilter = <T extends { id: string; tittel: string; nokkel?: InnsatsgruppeNokler }>({
  accordionNavn,
  option,
  setOption,
  data,
  isLoading,
  isError,
  defaultOpen = false,
}: InnsatsgruppeFilterProps<T>) => {
  const radiobox = (option: T) => {
    return (
      <Radio
        value={option.nokkel}
        key={`${option.id}`}
        data-testid={`filter_checkbox_${kebabCase(option?.tittel ?? '')}`}
      >
        {option.tittel}
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
