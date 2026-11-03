import { FilterAccordion, FilterSkeleton } from "@mr/frontend-common";
import { tilsagnFilterAccordionAtom } from "../../pages/gjennomforing/tilsagn/filter";
import { useAtom } from "jotai";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { Accordion } from "@navikt/ds-react";
import { ArrangorKobling } from "@tiltaksadministrasjon/api-client";
import { TilsagnFilterType } from "@/pages/gjennomforing/tilsagn/filter";
import { KostnadsstedFilter } from "./KostnadsstedFilter";
import { CheckboxList } from "./CheckboxList";
import { TILSAGN_STATUS_OPTIONS } from "@/utils/filterUtils";
import { type FilterKilde } from "@/sporing/useUmami";
import { useFilterTracking } from "@/sporing/useFilterTracking";

interface Props {
  filter: TilsagnFilterType;
  tilgjengeligeKostnadssteder: string[];
  updateFilter: (values: Partial<TilsagnFilterType>, kilde?: FilterKilde) => void;
}

export function TilsagnFilter({ filter, tilgjengeligeKostnadssteder, updateFilter }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(tilsagnFilterAccordionAtom);
  const { logAccordionEndret } = useFilterTracking("tilsagn");

  const { data: arrangorer } = useArrangorer(ArrangorKobling.TILTAKSGJENNOMFORING, {
    pageSize: 10000,
  });

  const toggleAccordion = (key: string, tittel: string) => {
    const opening = !accordionsOpen.includes(key);
    setAccordionsOpen([...addOrRemove(accordionsOpen, key)]);
    logAccordionEndret(tittel, opening);
  };

  const handleStatusChange = (status: (typeof TILSAGN_STATUS_OPTIONS)[number]["value"]) => {
    updateFilter({
      statuser: addOrRemove(filter.statuser, status),
      page: 1,
    });
  };

  const handleSelectAllStatuses = (checked: boolean) => {
    const statuser = checked ? TILSAGN_STATUS_OPTIONS.map((s) => s.value) : [];
    updateFilter({ statuser, page: 1 }, checked ? "velg_alle" : "fjern_alle");
  };

  const handleKostnadsstederChange = (kostnadssteder: string[]) => {
    updateFilter({ kostnadssteder, page: 1 });
  };

  if (!arrangorer) {
    return <FilterSkeleton />;
  }

  return (
    <Accordion size="small">
      <FilterAccordion
        tittel="Status"
        antallValgteFilter={filter.statuser.length}
        open={accordionsOpen.includes("status")}
        onClick={() => toggleAccordion("status", "Status")}
      >
        <CheckboxList
          onSelectAll={handleSelectAllStatuses}
          items={TILSAGN_STATUS_OPTIONS}
          isChecked={(status) => filter.statuser.includes(status)}
          onChange={handleStatusChange}
        />
      </FilterAccordion>
      <FilterAccordion
        tittel="Kostnadssted"
        open={accordionsOpen.includes("kostnadssted")}
        onClick={() => toggleAccordion("kostnadssted", "Kostnadssted")}
      >
        <KostnadsstedFilter
          value={filter.kostnadssteder}
          tilgjengeligeKostnadssteder={tilgjengeligeKostnadssteder}
          onChange={handleKostnadsstederChange}
          sporingskontekst="tilsagn"
        />
      </FilterAccordion>
    </Accordion>
  );
}
