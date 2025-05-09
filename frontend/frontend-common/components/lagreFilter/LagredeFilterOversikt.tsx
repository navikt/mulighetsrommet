import { TrashFillIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, HStack, Radio, RadioGroup } from "@navikt/ds-react";
import { useRef, useState } from "react";
import styles from "./LagredeFilterOversikt.module.scss";
import { VarselModal } from "../varsel/VarselModal";


interface LagretFilter {
    id: string;
    navn: string;
    filter: {
        [key: string]: unknown;
    };
}

interface Props {
  lagredeFilter: LagretFilter[];
  filter: any;
  setFilter: (filter: any) => void;
  validateFilterStructure: (filter: any) => boolean;
  onDelete: (id: string) => void;
}

export function LagredeFilterOversikt({
  lagredeFilter,
  filter,
  setFilter,
  validateFilterStructure,
  onDelete,
}: Props) {
  const [filterForSletting, setFilterForSletting] = useState<LagretFilter | undefined>(undefined);
  const [filterHarUgyldigStruktur, setFilterHarUgyldigStruktur] = useState<
    LagretFilter | undefined
  >(undefined);

  const sletteFilterModalRef = useRef<HTMLDialogElement>(null);
  const filterHarUgyldigStrukturModalRef = useRef<HTMLDialogElement>(null);

  function oppdaterFilter(id: string) {
    const valgtFilter = lagredeFilter.find((f) => f.id === id);
    if (valgtFilter) {
      if (!validateFilterStructure(valgtFilter.filter)) {
        setFilterHarUgyldigStruktur(valgtFilter);
      } else {
        setFilter({ ...valgtFilter.filter, lagretFilterIdValgt: valgtFilter.id });
      }
    }
  }

  function slettFilter(id: string) {
    onDelete(id);
    setFilter({ ...filter, lagretFilterIdValgt: undefined });
    setFilterForSletting(undefined);
    setFilterHarUgyldigStruktur(undefined);
    sletteFilterModalRef.current?.close();
    filterHarUgyldigStrukturModalRef.current?.close();
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
          headingText="Beklager, filteret fungerte ikke"
          modalRef={filterHarUgyldigStrukturModalRef}
          handleClose={() => {
            setFilterForSletting(undefined);
            filterHarUgyldigStrukturModalRef.current?.close();
          }}
          body={
            <>
              <BodyShort>
                På grunn av nylige endringer kan ikke det lagrede filteret benyttes lenger. Filteret
                bør derfor slettes og lagres på nytt.
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
