import { Accordion, Alert, Loader, Radio, RadioGroup } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { Innsatsgruppe } from 'mulighetsrommet-api-client';
import { logEvent } from '../../core/api/logger';
import { useInnsatsgrupper } from '../../core/api/queries/useInnsatsgrupper';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { kebabCase } from '../../utils/Utils';
import './Filtermeny.module.scss';

interface InnsatsgruppeFilterProps<T extends { id: string; tittel: string; nokkel?: Innsatsgruppe }> {
  accordionNavn: string;
  option?: Innsatsgruppe;
  setOption: (type: Innsatsgruppe) => void;
  options: T[];
  isLoading: boolean;
  isError: boolean;
  defaultOpen?: boolean;
}

const InnsatsgruppeAccordion = <T extends { id: string; tittel: string; nokkel?: Innsatsgruppe }>({
  accordionNavn,
  option,
  setOption,
  options,
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
    <Accordion.Item defaultOpen={defaultOpen}>
      <Accordion.Header data-testid={`filter_accordionheader_${kebabCase(accordionNavn)}`}>
        {accordionNavn}
      </Accordion.Header>
      <Accordion.Content data-testid={`filter_accordioncontent_${kebabCase(accordionNavn)}`}>
        {isLoading && <Loader size="xlarge" />}
        {options.length !== 0 && (
          <RadioGroup
            legend=""
            hideLegend
            size="small"
            onChange={(e: Innsatsgruppe) => {
              setOption(e);
            }}
            value={option ?? null}
          >
            {options.map(radiobox)}
          </RadioGroup>
        )}
        {isError && <Alert variant="error">Det har skjedd en feil</Alert>}
      </Accordion.Content>
    </Accordion.Item>
  );
};

function InnsatsgruppeFilter() {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const innsatsgrupper = useInnsatsgrupper();

  const handleEndreFilter = (innsatsgruppe: string) => {
    const foundInnsatsgruppe = innsatsgrupper.data?.find(gruppe => gruppe.nokkel === innsatsgruppe);
    if (foundInnsatsgruppe) {
      setFilter({
        ...filter,
        innsatsgruppe: {
          id: foundInnsatsgruppe._id,
          tittel: foundInnsatsgruppe.tittel,
          nokkel: foundInnsatsgruppe.nokkel,
        },
      });
    }
    logEvent('mulighetsrommet.filtrering', { type: 'innsatsgruppe', value: kebabCase(innsatsgruppe) });
  };

  const options = innsatsgrupper.data?.map(innsatsgruppe => {
    return { id: innsatsgruppe._id, tittel: innsatsgruppe.tittel, nokkel: innsatsgruppe.nokkel };
  });
  return (
    <InnsatsgruppeAccordion
      accordionNavn="Innsatsgruppe"
      option={filter.innsatsgruppe?.nokkel}
      setOption={innsatsgruppe => {
        handleEndreFilter(innsatsgruppe);
      }}
      options={options ?? []}
      isLoading={innsatsgrupper.isLoading}
      isError={innsatsgrupper.isError}
      defaultOpen
    />
  );
}

export default InnsatsgruppeFilter;
