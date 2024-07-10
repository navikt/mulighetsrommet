import { TrashFillIcon } from "@navikt/aksel-icons";
import {
  Accordion,
  Alert,
  BodyShort,
  Button,
  HGrid,
  HStack,
  Radio,
  RadioGroup,
} from "@navikt/ds-react";
import { LagretDokumenttype, LagretFilter } from "mulighetsrommet-api-client";
import { FilterAccordionHeader } from "mulighetsrommet-frontend-common";
import { useEffect, useRef, useState } from "react";
import { useGetLagredeFilterForDokumenttype } from "./getLagredeFilterForDokumenttype";
import { useSlettFilter } from "./useSlettFilter";
import { VarselModal } from "../../../mr-admin-flate/src/components/modal/VarselModal";
import styles from "./LagredeFilterOversikt.module.scss";
import _isEqual from "lodash.isequal";
import { useLagreFilter } from "./useLagreFilter";

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
  const lagreFilterMutation = useLagreFilter({ dokumenttype, onSuccess: () => {} });

  useEffect(() => {
    setOpenLagrede(lagredeFilter.length > 0);
  }, [lagredeFilter]);

  function oppdaterFilter(id: string) {
    const valgtFilter = lagredeFilter.find((f) => f.id === id);
    if (valgtFilter) {
      if (getDifferentKeys(valgtFilter.filter, filter).length === 0) {
        setFilter({ ...valgtFilter.filter, lagretFilterIdValgt: valgtFilter.id });
      } else {
        const ok = confirm(
          "Filterstrukturen har endret seg. Du må oppdatere for å bruke dette filteret. Vil du oppdatere?",
        );
        if (ok) {
          lagreFilterMutation.mutate({
            ...valgtFilter,
            filter: { ...valgtFilter.filter },
          });
          setFilter({ ...valgtFilter.filter, lagretFilterIdValgt: valgtFilter.id });
        }
      }
    }
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
                <Alert variant="info" inline>
                  Du har ingen lagrede filter
                </Alert>
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
        />
      ) : null}
    </>
  );
}

function getDifferentKeys<T extends object>(obj1: T, obj2: T): Array<keyof T> {
  const keysObj1 = new Set<keyof T>(Object.keys(obj1) as Array<keyof T>);
  const keysObj2 = new Set<keyof T>(Object.keys(obj2) as Array<keyof T>);
  const differentKeys: Array<keyof T> = [];

  keysObj1.forEach((key) => {
    if (!keysObj2.has(key)) {
      differentKeys.push(key);
    }
  });

  keysObj2.forEach((key) => {
    if (!keysObj1.has(key)) {
      differentKeys.push(key);
    }
  });

  return differentKeys;
}
