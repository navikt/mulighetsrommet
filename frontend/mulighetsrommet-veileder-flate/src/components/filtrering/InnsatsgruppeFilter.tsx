import { Accordion, HelpText, Radio, RadioGroup } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Innsatsgruppe, VeilederflateInnsatsgruppe } from "@mr/api-client-v2";
import { useInnsatsgrupper } from "@/api/queries/useInnsatsgrupper";
import { filterAccordionAtom } from "@/core/atoms";
import { useArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { addOrRemove } from "@/utils/Utils";
import { kebabCase } from "@mr/frontend-common/utils/TestUtils";
import { FilterAccordionHeader } from "@mr/frontend-common";

export function InnsatsgruppeFilter() {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();
  const innsatsgrupper = useInnsatsgrupper();

  const onChange = (innsatsgruppe: Innsatsgruppe) => {
    const foundInnsatsgruppe = innsatsgrupper.data?.find(
      (gruppe) => gruppe.nokkel === innsatsgruppe,
    );
    if (foundInnsatsgruppe) {
      setFilter({
        ...filter,
        innsatsgruppe: foundInnsatsgruppe,
      });
    }
  };

  return (
    <InnsatsgruppeAccordion
      value={filter.innsatsgruppe?.nokkel}
      onChange={onChange}
      options={innsatsgrupper.data ?? []}
    />
  );
}

interface InnsatsgruppeAccordionProps {
  value?: Innsatsgruppe;
  onChange: (type: Innsatsgruppe) => void;
  options: VeilederflateInnsatsgruppe[];
}

function InnsatsgruppeAccordion({ value, onChange, options }: InnsatsgruppeAccordionProps) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(filterAccordionAtom);

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
              onChange(e);
            }}
            value={value ?? null}
          >
            {options.map((option) => {
              return (
                <Radio
                  key={option.nokkel}
                  value={option.nokkel}
                  data-testid={`filter_radio_${kebabCase(option.nokkel)}`}
                >
                  {option.tittel}
                </Radio>
              );
            })}
          </RadioGroup>
        )}
      </Accordion.Content>
    </Accordion.Item>
  );
}
