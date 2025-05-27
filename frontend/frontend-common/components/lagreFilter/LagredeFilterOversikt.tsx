import { StarFillIcon, StarIcon, TrashFillIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, HStack, Radio, RadioGroup } from "@navikt/ds-react";
import { useRef, useState } from "react";
import styles from "./LagredeFilterOversikt.module.scss";
import { VarselModal } from "../varsel/VarselModal";

type FilterValues = { [key: string]: unknown };

interface LagretFilter {
  id: string;
  navn: string;
  filter: FilterValues;
  isDefault: boolean;
}

interface Props {
  filters: LagretFilter[];
  selectedFilterId: string | undefined;
  onSelectFilterId: (id: string) => void;
  onDeleteFilter: (id: string) => void;
  onSetDefaultFilter: (id: string, isDefault: boolean) => void;
  validateFilterStructure: (filter: FilterValues) => boolean;
}

export function LagredeFilterOversikt({
  filters,
  selectedFilterId,
  onSelectFilterId,
  validateFilterStructure,
  onDeleteFilter,
  onSetDefaultFilter,
}: Props) {
  const [filterForSletting, setFilterForSletting] = useState<LagretFilter | null>();
  const sletteFilterModalRef = useRef<HTMLDialogElement>(null);

  const filterHarUgyldigStrukturModalRef = useRef<HTMLDialogElement>(null);
  const [filterHarUgyldigStruktur, setFilterHarUgyldigStruktur] = useState<LagretFilter | null>();

  function oppdaterFilter(id: string) {
    const selectedFilter = filters.find((f) => f.id === id);

    if (selectedFilter !== undefined && !validateFilterStructure(selectedFilter.filter)) {
      setFilterHarUgyldigStruktur(selectedFilter);
    } else {
      onSelectFilterId(id);
    }
  }

  function slettFilter(id: string) {
    onDeleteFilter(id);

    setFilterForSletting(null);
    sletteFilterModalRef.current?.close();

    setFilterHarUgyldigStruktur(null);
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
      {filters.length === 0 ? (
        <Alert variant="info" inline>
          Du har ingen lagrede filter
        </Alert>
      ) : (
        <RadioGroup
          legend="Lagrede filter"
          hideLegend
          onChange={(id) => oppdaterFilter(id)}
          value={selectedFilterId || null}
        >
          <div className={styles.overflow}>
            {filters.map((lagretFilter) => {
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
                  <div className={styles.filterActions}>
                    <Button
                      icon={lagretFilter.isDefault ? <StarFillIcon /> : <StarIcon />}
                      iconPosition="right"
                      aria-label={
                        lagretFilter.isDefault ? "Fjern som favoritt" : "Velg som favoritt"
                      }
                      variant="tertiary"
                      size="medium"
                      onClick={() => {
                        onSetDefaultFilter(lagretFilter.id, !lagretFilter.isDefault);
                      }}
                    />
                    <Button
                      icon={<TrashFillIcon />}
                      iconPosition="right"
                      aria-label="Slett filter"
                      variant="tertiary-neutral"
                      size="medium"
                      onClick={() => {
                        setFilterForSletting(lagretFilter);
                      }}
                    />
                  </div>
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
          body={sletteBody(filterForSletting.navn)}
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
