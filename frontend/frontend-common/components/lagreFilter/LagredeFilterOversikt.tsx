import { StarFillIcon, StarIcon, TrashFillIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, HStack, Radio, RadioGroup, Tooltip } from "@navikt/ds-react";
import { useRef, useState } from "react";
import { VarselModal } from "../varsel/VarselModal";

interface LagretFilter {
  id: string;
  navn: string;
  isDefault: boolean;
}

interface Props {
  filters: LagretFilter[];
  selectedFilterId: string | undefined;
  onSelectFilterId: (id: string) => void;
  onDeleteFilter: (id: string) => void;
  onSetDefaultFilter: (id: string, isDefault: boolean) => void;
}

export function LagredeFilterOversikt({
  filters,
  selectedFilterId,
  onSelectFilterId,
  onDeleteFilter,
  onSetDefaultFilter,
}: Props) {
  const [filterForSletting, setFilterForSletting] = useState<LagretFilter | null>();
  const sletteFilterModalRef = useRef<HTMLDialogElement>(null);

  function slettFilter(id: string) {
    onDeleteFilter(id);

    setFilterForSletting(null);
    sletteFilterModalRef.current?.close();
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
      <Button
        data-color="danger"
        variant="primary"
        onClick={() => slettFilter(id)}
        icon={<TrashFillIcon />}
      >
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
          onChange={(id) => onSelectFilterId(id)}
          value={selectedFilterId || null}
        >
          <div>
            {filters.map((lagretFilter) => {
              const defaultFilterLabel = lagretFilter.isDefault
                ? "Fjern som favoritt"
                : "Velg som favoritt";
              const deleteFilterLabel = "Slett filter";
              return (
                <HStack
                  key={lagretFilter.id}
                  justify="space-between"
                  wrap={false}
                  gap="space-8"
                  align="start" // align="start" so icons stay pinned to the top when the label wraps to 2 lines
                >
                  <Radio
                    size="small"
                    value={lagretFilter.id}
                    style={{ minWidth: 0, flex: "1 1 auto" }} // lets the label wrap instead of squeezing siblings
                  >
                    {lagretFilter.navn}
                  </Radio>
                  <HStack
                    wrap={false}
                    gap="space-4"
                    flexShrink="0" // icon group keeps its size, never gets compressed
                  >
                    <Tooltip content={defaultFilterLabel}>
                      <Button
                        icon={lagretFilter.isDefault ? <StarFillIcon /> : <StarIcon />}
                        iconPosition="right"
                        aria-label={defaultFilterLabel}
                        variant="tertiary"
                        size="small"
                        onClick={() => onSetDefaultFilter(lagretFilter.id, !lagretFilter.isDefault)}
                      />
                    </Tooltip>
                    <Tooltip content={deleteFilterLabel}>
                      <Button
                        data-color="neutral"
                        icon={<TrashFillIcon />}
                        iconPosition="right"
                        aria-label={deleteFilterLabel}
                        variant="tertiary"
                        size="small"
                        onClick={() => setFilterForSletting(lagretFilter)}
                      />
                    </Tooltip>
                  </HStack>
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
    </>
  );
}
