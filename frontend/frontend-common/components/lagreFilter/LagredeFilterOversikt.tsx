import { TrashFillIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, HStack, Radio, RadioGroup } from "@navikt/ds-react";
import { LagretDokumenttype, LagretFilter } from "@mr/api-client";
import { useRef, useState } from "react";
import { useGetLagredeFilterForDokumenttype } from "./useGetLagredeFilterForDokumenttype";
import { useUpdateSistBruktTimestampForLagretFilter } from "./useUpdateSistBruktTimestampForLagretFilter";
import styles from "./LagredeFilterOversikt.module.scss";
import { useSlettFilter } from "./useSlettFilter";
import { VarselModal } from "../varsel/VarselModal";

interface Props {
  dokumenttype: LagretDokumenttype;
  filter: any;
  setFilter: (filter: any) => void;
  validateFilterStructure: (filter: any) => boolean;
}

export function LagredeFilterOversikt({
  dokumenttype,
  filter,
  setFilter,
  validateFilterStructure,
}: Props) {
  const { data: lagredeFilter = [] } = useGetLagredeFilterForDokumenttype(dokumenttype);
  const updateSistBruktTimestampMutation = useUpdateSistBruktTimestampForLagretFilter({
    dokumenttype,
  });
  const [filterForSletting, setFilterForSletting] = useState<LagretFilter | undefined>(undefined);
  const [filterHarUgyldigStruktur, setFilterHarUgyldigStruktur] = useState<
    LagretFilter | undefined
  >(undefined);

  const sletteFilterModalRef = useRef<HTMLDialogElement>(null);
  const filterHarUgyldigStrukturModalRef = useRef<HTMLDialogElement>(null);
  const mutation = useSlettFilter(dokumenttype);

  function oppdaterFilter(id: string) {
    const valgtFilter = lagredeFilter.find((f) => f.id === id);
    if (valgtFilter) {
      if (!validateFilterStructure(valgtFilter.filter)) {
        setFilterHarUgyldigStruktur(valgtFilter);
      } else {
        updateSistBruktTimestampMutation.mutate(id, {
          onSettled: () => {
            setFilter({ ...valgtFilter.filter, lagretFilterIdValgt: valgtFilter.id });
          },
        });
      }
    }
  }

  function slettFilter(id: string) {
    mutation.mutate(id, {
      onSuccess: () => {
        setFilter({ ...filter, lagretFilterIdValgt: undefined });
        setFilterForSletting(undefined);
        setFilterHarUgyldigStruktur(undefined);
        sletteFilterModalRef.current?.close();
        filterHarUgyldigStrukturModalRef.current?.close();
      },
    });
  }

  const sletteBody = (typeFilter: string) => {
    return (
      <BodyShort>
        Vil du slette filteret: <b>{typeFilter}</b> ?
      </BodyShort>
    );
  };

  const sletteKnapp = (id: string) => {
    return (
      <Button variant="danger" onClick={() => slettFilter(id)} icon={<TrashFillIcon />}>
        Ja, jeg vil slette
      </Button>
    );
  };

  return (
    <>
      {lagredeFilter.length === 0 ? (
        <Alert variant="info" inline>
          Du har ingen lagrede filter
        </Alert>
      ) : (
        <RadioGroup
          legend="Lagrede filter"
          hideLegend
          onChange={(id) => oppdaterFilter(id)}
          value={filter.lagretFilterIdValgt ? filter.lagretFilterIdValgt : null}
        >
          <div className={styles.overflow}>
            {lagredeFilter?.map((lagretFilter) => {
              return (
                <HStack
                  key={lagretFilter.id}
                  justify="space-between"
                  wrap={false}
                  gap="2"
                  align="center"
                >
                  <Radio size="small" value={lagretFilter.id}>
                    {lagretFilter.navn}
                  </Radio>
                  <Button
                    className={styles.sletteknapp}
                    icon={<TrashFillIcon />}
                    iconPosition="right"
                    aria-label="Slett filter"
                    variant="tertiary-neutral"
                    size="medium"
                    onClick={() => {
                      setFilterForSletting(lagredeFilter.find((f) => f.id === lagretFilter.id));
                    }}
                  />
                </HStack>
              );
            })}
          </div>
        </RadioGroup>
      )}

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
          body={sletteBody(filterForSletting?.navn)}
          primaryButton={sletteKnapp(filterForSletting.id)}
          secondaryButton
          secondaryButtonHandleAction={() => sletteFilterModalRef.current?.close()}
        />
      ) : null}

      {filterHarUgyldigStruktur ? (
        <VarselModal
          open={!!filterHarUgyldigStruktur}
          headingIconType="warning"
          headingText="Filteret er ugyldig"
          modalRef={filterHarUgyldigStrukturModalRef}
          handleClose={() => {
            setFilterForSletting(undefined);
            filterHarUgyldigStrukturModalRef.current?.close();
          }}
          body={
            <>
              <BodyShort>
                Det lagrede filteret har en ugyldig struktur og kan ikke lastes inn. Du kan slette
                filteret og lagre et nytt med samme navn.
              </BodyShort>
              {sletteBody(filterHarUgyldigStruktur?.navn)}
            </>
          }
          primaryButton={sletteKnapp(filterHarUgyldigStruktur.id)}
          secondaryButton
          secondaryButtonHandleAction={() => {
            setFilterHarUgyldigStruktur(undefined);
            filterHarUgyldigStrukturModalRef.current?.close();
          }}
        />
      ) : null}
    </>
  );
}
