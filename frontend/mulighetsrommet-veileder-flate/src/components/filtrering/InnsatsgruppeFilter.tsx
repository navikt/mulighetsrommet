import { Accordion, Alert, HelpText, Radio, RadioGroup } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Innsatsgruppe } from "mulighetsrommet-api-client";
import { useInnsatsgrupper } from "@/core/api/queries/useInnsatsgrupper";
import { filterAccordionAtom } from "@/core/atoms/atoms";
import { useArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { addOrRemove } from "@/utils/Utils";
import "./Filtermeny.module.scss";
import { FilterAccordionHeader } from "mulighetsrommet-frontend-common/components/filter/accordionHeader/FilterAccordionHeader";
import { kebabCase } from "mulighetsrommet-frontend-common/utils/Utils";

interface InnsatsgruppeFilterProps<
  T extends { id: string; tittel: string; nokkel?: Innsatsgruppe },
> {
  option?: Innsatsgruppe;
  setOption: (type: Innsatsgruppe) => void;
  options: T[];
  isError: boolean;
}

const InnsatsgruppeAccordion = <T extends { id: string; tittel: string; nokkel?: Innsatsgruppe }>({
  option,
  setOption,
  options,
  isError,
}: InnsatsgruppeFilterProps<T>) => {
  const [accordionsOpen, setAccordionsOpen] = useAtom(filterAccordionAtom);

  const radiobox = (option: T) => {
    return (
      <Radio
        value={option.nokkel}
        key={`${option.id}`}
        data-testid={`filter_radio_${kebabCase(option?.tittel ?? "")}`}
      >
        {option.tittel}
      </Radio>
    );
  };

  return (
    <Accordion.Item open={accordionsOpen.includes("innsatsgruppe")}>
      <Accordion.Header
        onClick={() => {
          setAccordionsOpen([...addOrRemove(accordionsOpen, "innsatsgruppe")]);
        }}
        data-testid="filter_accordionheader_innsatsgruppe"
      >
        <FilterAccordionHeader
          tittel="Innsatsgruppe"
          tilleggsinformasjon={
            <HelpText placement="right" strategy="fixed" onClick={(e) => e.stopPropagation()}>
              Filteret viser de tiltakene som kan være aktuelle for brukere med valgt innsatsgruppe.
              <br />
              <a
                href="https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-tiltak-og-virkemidler/SitePages/Tiltak-og-virkemidler-etter-innsatsbehov.aspx"
                rel="noreferrer"
                target="_blank"
              >
                Se mer informasjon om innsatsgruppe og tiltak her.
              </a>
            </HelpText>
          }
        />
      </Accordion.Header>
      <Accordion.Content data-testid="filter_accordioncontent_innsatsgruppe">
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
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();
  const innsatsgrupper = useInnsatsgrupper();

  const handleEndreFilter = (innsatsgruppe: string) => {
    const foundInnsatsgruppe = innsatsgrupper.data?.find(
      (gruppe) => gruppe.nokkel === innsatsgruppe,
    );
    if (foundInnsatsgruppe) {
      setFilter({
        ...filter,
        innsatsgruppe: {
          id: foundInnsatsgruppe.sanityId,
          tittel: foundInnsatsgruppe.tittel,
          nokkel: foundInnsatsgruppe.nokkel,
        },
      });
    }
  };

  const options = innsatsgrupper.data?.map((innsatsgruppe) => {
    return {
      id: innsatsgruppe.sanityId,
      tittel: innsatsgruppe.tittel,
      nokkel: innsatsgruppe.nokkel,
    };
  });
  return (
    <InnsatsgruppeAccordion
      option={filter.innsatsgruppe?.nokkel}
      setOption={(innsatsgruppe) => {
        handleEndreFilter(innsatsgruppe);
      }}
      options={options ?? []}
      isError={innsatsgrupper.isError}
    />
  );
}

export default InnsatsgruppeFilter;
