import { WritableAtom, useAtom } from "jotai";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { ArrangorerFilter, defaultArrangorerFilter } from "../../api/atoms";

interface Props {
  filterAtom: WritableAtom<ArrangorerFilter, [newValue: ArrangorerFilter], void>;
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
