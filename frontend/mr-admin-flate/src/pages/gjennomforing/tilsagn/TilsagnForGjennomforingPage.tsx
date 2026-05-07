import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { GjennomforingHandling, TilsagnType } from "@tiltaksadministrasjon/api-client";
import { useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import { useTilsagnTableData } from "@/pages/gjennomforing/tilsagn/detaljer/tilsagnDetaljerLoader";
import { TilsagnTable } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnTable";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Handlinger } from "@/components/handlinger/Handlinger";

export function TilsagnForGjennomforingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const handlinger = useGjennomforingHandlinger(gjennomforingId);
  const { data: tilsagn } = useTilsagnTableData(gjennomforingId);

  return (
    <>
      <KnapperadContainer>
        <Handlinger
          handlinger={handlinger}
          grupper={[
            {
              items: [
                {
                  label: `Opprett ${avtaletekster.tilsagn.type(TilsagnType.TILSAGN).toLowerCase()}`,
                  href: `opprett-tilsagn?type=${TilsagnType.TILSAGN}`,
                  handling: GjennomforingHandling.OPPRETT_TILSAGN,
                },
                {
                  label: `Opprett ${avtaletekster.tilsagn.type(TilsagnType.EKSTRATILSAGN).toLowerCase()}`,
                  href: `opprett-tilsagn?type=${TilsagnType.EKSTRATILSAGN}`,
                  handling: GjennomforingHandling.OPPRETT_EKSTRATILSAGN,
                },
                {
                  label: `Opprett ${avtaletekster.tilsagn.type(TilsagnType.INVESTERING).toLowerCase()}`,
                  href: `opprett-tilsagn?type=${TilsagnType.INVESTERING}`,
                  handling: GjennomforingHandling.OPPRETT_TILSAGN_FOR_INVESTERINGER,
                },
              ],
            },
          ]}
        />
      </KnapperadContainer>
      <TilsagnTable
        emptyStateMessage="Det finnes ingen tilsagn for dette tiltaket"
        data={tilsagn}
      />
    </>
  );
}
