import { Button } from "@navikt/ds-react";
import { useAtom, WritableAtom } from "jotai";
import { AvtaleFilter, defaultAvtaleFilter } from "../../api/atoms";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";

interface Props {
  filterAtom: WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>;
  tiltakstypeId?: string;
}

export function AvtaleFilterButtons({ filterAtom, tiltakstypeId }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "row",
        justifyContent: "space-between",
        height: "100%",
        alignItems: "center",
      }}
    >
      {filter.sok.length > 0 ||
      filter.navRegioner.length > 0 ||
      (!tiltakstypeId && filter.tiltakstyper.length > 0) ||
      filter.statuser.length > 0 ||
      filter.leverandor.length > 0 ? (
        <Button
          type="button"
          size="small"
          style={{ maxWidth: "130px" }}
          variant="tertiary"
          onClick={() => {
            setFilter({
              ...defaultAvtaleFilter,
              tiltakstyper: tiltakstypeId ? [tiltakstypeId] : defaultAvtaleFilter.tiltakstyper,
            });
          }}
        >
          Nullstill filter
        </Button>
      ) : (
        <div></div>
      )}
      {/*
        Empty div over for å dytte de andre knappene til høyre uavhengig
        av om nullstill knappen er der
      */}
      <Lenkeknapp to="/avtaler/skjema" size="small" variant="primary">
        Opprett ny avtale
      </Lenkeknapp>
    </div>
  );
}
