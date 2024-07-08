import { TrashFillIcon } from "@navikt/aksel-icons";
import { Accordion, BodyShort, Button, HGrid, HStack, Radio, RadioGroup } from "@navikt/ds-react";
import { LagretDokumenttype, LagretFilter } from "mulighetsrommet-api-client";
import { FilterAccordionHeader } from "mulighetsrommet-frontend-common";
import { useEffect, useRef, useState } from "react";
import { useGetLagredeFilterForDokumenttype } from "../../api/lagretFilter/getLagredeFilterForDokumenttype";
import { useSlettFilter } from "../../api/lagretFilter/useSlettFilter";
import { VarselModal } from "../modal/VarselModal";
import styles from "./LagredeFilterOversikt.module.scss";

interface Props {
  dokumenttype: LagretDokumenttype;
  filter: any;
  setFilter: (filter: any) => void;
}

export function LagredeFilterOversikt({ dokumenttype, filter, setFilter }: Props) {
  const { data: lagredeFilter = [] } = useGetLagredeFilterForDokumenttype(dokumenttype);
  const [openLagrede, setOpenLagrede] = useState(lagredeFilter.length > 0);
  const [filterForSletting, setFilterForSletting] = useState<LagretFilter | undefined>(undefined);
  const sletteFilterModalRef = useRef<HTMLDialogElement>(null);
  const mutation = useSlettFilter(dokumenttype);

  useEffect(() => {
    setOpenLagrede(lagredeFilter.length > 0);
  }, [lagredeFilter]);

  function oppdaterFilter(id: string) {
    const valgtFilter = lagredeFilter.find((f) => f.id === id);
    setFilter({ ...valgtFilter?.filter, lagretFilterIdValgt: valgtFilter?.id });
  }

  function slettFilter(id: string) {
    if (filterForSletting) {
      {
        mutation.mutate(id);
        setFilter({ ...filter, lagretFilterIdValgt: undefined });
        sletteFilterModalRef.current?.close();
      }
    }
  }

  return (
    <>
      <Accordion>
        <Accordion.Item open={openLagrede} onOpenChange={() => setOpenLagrede(!openLagrede)}>
          <Accordion.Header>
            <FilterAccordionHeader
              tittel="Lagrede filter"
              antallValgteFilter={filter.lagretFilterIdValgt ? 1 : 0}
            />
          </Accordion.Header>
          <Accordion.Content>
            <>
              {lagredeFilter.length === 0 ? (
                <BodyShort>Du har ingen lagrede filter</BodyShort>
              ) : (
                <RadioGroup
                  legend="Mine filter"
                  hideLegend
                  onChange={(id) => oppdaterFilter(id)}
                  value={filter.lagretFilterIdValgt ? filter.lagretFilterIdValgt : null}
                >
                  <div className={styles.overflow}>
                    {lagredeFilter?.map((lagretFilter) => {
                      return (
                        <HGrid key={lagretFilter.id} align={"start"} columns={"10rem auto"}>
                          <Radio size="small" value={lagretFilter.id}>
                            {lagretFilter.navn}
                          </Radio>
                          <Button
                            aria-label="Slett filter"
                            variant="tertiary-neutral"
                            size="small"
                            onClick={() => {
                              setFilterForSletting(
                                lagredeFilter.find((f) => f.id === lagretFilter.id),
                              );
                            }}
                          >
                            <TrashFillIcon />
                          </Button>
                        </HGrid>
                      );
                    })}
                  </div>
                </RadioGroup>
              )}
            </>
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
      {filterForSletting ? (
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
              Vil du slette filteret: <b>{filterForSletting?.navn}</b>
            </BodyShort>
          }
          primaryButton={
            <Button variant="danger" size="small" onClick={() => slettFilter(filterForSletting.id)}>
              <HStack align="center">
                <TrashFillIcon /> Slett
              </HStack>
            </Button>
          }
          secondaryButton
          secondaryButtonHandleAction={() => sletteFilterModalRef.current?.close()}
        ></VarselModal>
      ) : null}
    </>
  );
}