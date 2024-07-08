import { TrashFillIcon } from "@navikt/aksel-icons";
import { Accordion, BodyShort, Button, HGrid, HStack, Radio, RadioGroup } from "@navikt/ds-react";
import isEqual from "lodash.isequal";
import { LagretDokumenttype, LagretFilter } from "mulighetsrommet-api-client";
import { FilterAccordionHeader } from "mulighetsrommet-frontend-common";
import { useEffect, useRef, useState } from "react";
import { useFilterBasedOnDokumenttype } from "../../api/atoms";
import { useGetLagredeFilterForDokumenttype } from "../../api/lagretFilter/getLagredeFilterForDokumenttype";
import { useSlettFilter } from "../../api/lagretFilter/useSlettFilter";
import { VarselModal } from "../modal/VarselModal";

interface Props {
  dokumenttype: LagretDokumenttype;
  setFilter: (filter: any) => void;
}

export function LagredeFilterOversikt({ dokumenttype, setFilter }: Props) {
  const filter = useFilterBasedOnDokumenttype(dokumenttype);
  const { data: lagredeFilter = [] } = useGetLagredeFilterForDokumenttype(dokumenttype);
  const [valgtFilter, setValgtFilter] = useState<LagretFilter | undefined>(undefined);
  const [filterForSletting, setFilterForSletting] = useState<LagretFilter | undefined>(undefined);
  const sletteFilterModalRef = useRef<HTMLDialogElement>(null);
  const mutation = useSlettFilter(LagretDokumenttype.AVTALE);

  useEffect(() => {
    lagredeFilter.forEach((f) => {
      if (isEqual(filter, f.filter)) {
        setValgtFilter(f);
      }
    });
  }, [filter, lagredeFilter]);

  function oppdaterFilter(filterValgt: any) {
    setFilter(filterValgt);
  }

  function slettFilter() {
    if (filterForSletting) {
      {
        mutation.mutate(filterForSletting.id);
        setValgtFilter(undefined);
        sletteFilterModalRef.current?.close();
      }
    }
  }

  return (
    <>
      <Accordion>
        <Accordion.Item defaultOpen={!!valgtFilter}>
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
                  value={valgtFilter?.filter}
                >
                  {lagredeFilter?.map((filter) => {
                    return (
                      <HGrid key={filter.id} align={"start"} columns={"10rem auto"}>
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
                      </HGrid>
                    );
                  })}
                </RadioGroup>
              )}
            </>
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
      <VarselModal
        open={!!filterForSletting}
        headingIconType="warning"
        headingText="Slette filter?"
        modalRef={sletteFilterModalRef}
        handleClose={() => {
          setFilterForSletting(undefined);
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
