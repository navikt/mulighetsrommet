import { useSortableData } from "@mr/frontend-common";
import { InfoCard } from "@navikt/ds-react";
import { useArrangorflateTilsagnRader } from "~/hooks/useArrangorflateTilsagnRader";
import { Tabellvisning } from "../common/Tabellvisning";
import { tilsagnKolonner, TilsagnRow } from "../common/TilsagnRow";

export function TilsagnTabell() {
  const { data: tilsagnRader } = useArrangorflateTilsagnRader();

  const { sortedData, sort, toggleSort } = useSortableData(tilsagnRader, undefined, (item, key) => {
    if (key === "periode") {
      return item[key].start;
    }
    return (item as Record<string, unknown>)[key];
  });

  if (!tilsagnRader.length) {
    return (
      <InfoCard data-color="warning" className="my-10">
        <InfoCard.Header>
          <InfoCard.Title>Det finnes ingen tilsagn her</InfoCard.Title>
        </InfoCard.Header>
      </InfoCard>
    );
  }

  return (
    <Tabellvisning kolonner={tilsagnKolonner} sort={sort} onSortChange={toggleSort}>
      {sortedData.map((rad) => (
        <TilsagnRow key={rad.id} row={rad} />
      ))}
    </Tabellvisning>
  );
}
