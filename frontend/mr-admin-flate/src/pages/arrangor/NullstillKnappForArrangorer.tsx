import { WritableAtom, useAtom } from "jotai";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { ArrangorerFilterType, defaultArrangorerFilter } from "@/api/atoms";

interface Props {
  filterAtom: WritableAtom<ArrangorerFilterType, [newValue: ArrangorerFilterType], void>;
}

export function NullstillKnappForArrangorer({ filterAtom }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);

  return (
    <div>
      {filter.sok.length > 0 ? (
        <NullstillFilterKnapp onClick={() => setFilter({ ...defaultArrangorerFilter })} />
      ) : null}
    </div>
  );
}
