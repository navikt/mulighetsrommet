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

interface Props {
  filter: TilsagnFilterType;
  tilgjengeligeKostnadssteder: string[];
  updateFilter: (values: Partial<TilsagnFilterType>) => void;
}

export function TilsagnFilter({ filter, tilgjengeligeKostnadssteder, updateFilter }: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(tilsagnFilterAccordionAtom);

  const { data: arrangorer } = useArrangorer(ArrangorKobling.TILTAKSGJENNOMFORING, {
    pageSize: 10000,
  });

  const toggleAccordion = (key: string) => {
    setAccordionsOpen([...addOrRemove(accordionsOpen, key)]);
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
        onClick={() => toggleAccordion("status")}
      >
        <CheckboxList
          onSelectAll={(checked) =>
            updateFilter({
              statuser: checked ? TILSAGN_STATUS_OPTIONS.map((s) => s.value) : [],
              page: 1,
            })
          }
          items={TILSAGN_STATUS_OPTIONS}
          isChecked={(status) => filter.statuser.includes(status)}
          onChange={(status) =>
            updateFilter({
              statuser: addOrRemove(filter.statuser, status),
              page: 1,
            })
          }
        />
      </FilterAccordion>
      <FilterAccordion
        tittel="Kostnadssted"
        open={accordionsOpen.includes("kostnadssted")}
        onClick={() => {
          setAccordionsOpen([...addOrRemove(accordionsOpen, "kostnadssted")]);
        }}
      >
        <KostnadsstedFilter
          value={filter.kostnadssteder}
          tilgjengeligeKostnadssteder={tilgjengeligeKostnadssteder}
          onChange={(kostnadssteder) => {
            updateFilter({ kostnadssteder, page: 1 });
          }}
        />
      </FilterAccordion>
    </Accordion>
  );
}
