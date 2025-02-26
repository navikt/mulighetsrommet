import { AvtaleFilter, defaultAvtaleFilter } from "@/api/atoms";
import { LagretDokumenttype } from "@mr/api-client-v2";
import { LagreFilterButton } from "@mr/frontend-common/components/lagreFilter/LagreFilterButton";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { HStack } from "@navikt/ds-react";
import { WritableAtom } from "jotai";
import { useAtom } from "jotai/index";
import { useFetcher } from "react-router";

interface Props {
  filterAtom: WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>;
  tiltakstypeId?: string;
}

export function NullstillKnappForAvtaler({ filterAtom, tiltakstypeId }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const fetcher = useFetcher();

  return (
    <div className="grid grid-cols-[auto auto] h-[100%] items-center">
      {filter.visMineAvtaler ||
      filter.sok.length > 0 ||
      filter.navRegioner.length > 0 ||
      filter.avtaletyper.length > 0 ||
      (!tiltakstypeId && filter.tiltakstyper.length > 0) ||
      filter.statuser.length > 0 ||
      filter.personvernBekreftet !== undefined ||
      filter.arrangorer.length > 0 ? (
        <HStack gap="2">
          <NullstillFilterKnapp
            onClick={() => {
              setFilter({
                ...defaultAvtaleFilter,
                tiltakstyper: tiltakstypeId ? [tiltakstypeId] : defaultAvtaleFilter.tiltakstyper,
              });
            }}
          />
          <LagreFilterButton
            onLagre={(r) => {
              const formData = new FormData();
              if (r.id) {
                formData.append("id", r.id);
              }
              formData.append("type", r.type);
              formData.append("filter", JSON.stringify(r.filter));
              formData.append("sortOrder", r.sortOrder.toString());
              formData.append("navn", r.navn);
              fetcher.submit(formData, {
                method: "POST",
                action: "/avtaler",
              });
            }}
            dokumenttype={LagretDokumenttype.AVTALE}
            filter={filter}
          />
        </HStack>
      ) : null}
    </div>
  );
}
