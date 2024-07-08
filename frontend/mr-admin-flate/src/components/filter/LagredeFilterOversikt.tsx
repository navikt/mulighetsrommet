import { TrashFillIcon } from "@navikt/aksel-icons";
import { Accordion, BodyShort, Button, HStack, Radio, RadioGroup } from "@navikt/ds-react";
import { LagretDokumenttype, LagretFilter } from "mulighetsrommet-api-client";
import { FilterAccordionHeader } from "mulighetsrommet-frontend-common";
import { useRef, useState } from "react";
import { useGetLagredeFilterForDokumenttype } from "../../api/lagretFilter/getLagredeFilterForDokumenttype";
import { VarselModal } from "../modal/VarselModal";
import { useSlettFilter } from "../../api/lagretFilter/useSlettFilter";

interface Props {
  dokumenttype: LagretDokumenttype;
  setFilter: (filter: any) => void;
}

export function LagredeFilterOversikt({ dokumenttype, setFilter }: Props) {
  const { data: lagredeFilter = [] } = useGetLagredeFilterForDokumenttype(dokumenttype);
  const [valgtFilter, setValgtFilter] = useState<LagretFilter | undefined>(undefined);
  const sletteFilterModalRef = useRef<HTMLDialogElement>(null);
  const mutation = useSlettFilter(LagretDokumenttype.AVTALE);

  function oppdaterFilter(filterValgt: any) {
    setFilter(filterValgt);
  }

  function slettFilter() {
    if (valgtFilter) {
      {
        mutation.mutate(valgtFilter.id);
        setValgtFilter(undefined);
        sletteFilterModalRef.current?.close();
      }
    }
  }

  return (
    <>
      <Accordion>
        <Accordion.Item>
          <Accordion.Header>
            <FilterAccordionHeader tittel="Lagrede filter" antallValgteFilter={0} />
          </Accordion.Header>
          <Accordion.Content>
            <>
              {lagredeFilter.length === 0 ? (
                <BodyShort>Du har ingen lagrede filter</BodyShort>
              ) : (
                <RadioGroup
                  legend="Mine filter"
                  hideLegend
                  onChange={(filterValgt) => oppdaterFilter(filterValgt)}
                >
                  {lagredeFilter?.map((filter) => {
                    return (
                      <HStack key={filter.id} align={"center"} justify={"space-between"}>
                        <Radio size="small" value={filter.filter}>
                          {filter.navn}
                        </Radio>
                        <Button
                          variant="tertiary-neutral"
                          size="small"
                          onClick={() => {
                            setValgtFilter(lagredeFilter.find((f) => f.id === filter.id));
                          }}
                        >
                          <TrashFillIcon />
                        </Button>
                      </HStack>
                    );
                  })}
                </RadioGroup>
              )}
            </>
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
      <VarselModal
        open={!!valgtFilter}
        headingIconType="warning"
        headingText="Slette filter?"
        modalRef={sletteFilterModalRef}
        handleClose={() => {
          setValgtFilter(undefined);
          sletteFilterModalRef.current?.close();
        }}
        body={
          <BodyShort>
            Vil du slette <b>{valgtFilter?.navn}</b>
          </BodyShort>
        }
        primaryButton={
          <Button variant="danger" size="small" onClick={slettFilter}>
            <HStack align="center">
              <TrashFillIcon /> Slett
            </HStack>
          </Button>
        }
      ></VarselModal>
    </>
  );
}
