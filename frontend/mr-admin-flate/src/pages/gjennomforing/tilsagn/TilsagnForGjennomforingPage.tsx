import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { GjennomforingHandling, TilsagnType } from "@tiltaksadministrasjon/api-client";
import { ActionMenu } from "@navikt/ds-react";
import { useNavigate } from "react-router";
import { useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import { useTilsagnTableData } from "@/pages/gjennomforing/tilsagn/detaljer/tilsagnDetaljerLoader";
import { TilsagnTable } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnTable";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Handlinger } from "@/components/handlinger/Handlinger";

export function TilsagnForGjennomforingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const handlinger = useGjennomforingHandlinger(gjennomforingId);
  const { data: tilsagn } = useTilsagnTableData(gjennomforingId);
  const navigate = useNavigate();

  return (
    <>
      <KnapperadContainer>
        <Handlinger>
          {handlinger.includes(GjennomforingHandling.OPPRETT_TILSAGN) && (
            <ActionMenu.Item
              onClick={() => {
                navigate(`opprett-tilsagn?type=${TilsagnType.TILSAGN}`);
              }}
            >
              Opprett {avtaletekster.tilsagn.type(TilsagnType.TILSAGN).toLowerCase()}
            </ActionMenu.Item>
          )}
          {handlinger.includes(GjennomforingHandling.OPPRETT_EKSTRATILSAGN) && (
            <ActionMenu.Item
              onClick={() => {
                navigate(`opprett-tilsagn?type=${TilsagnType.EKSTRATILSAGN}`);
              }}
            >
              Opprett {avtaletekster.tilsagn.type(TilsagnType.EKSTRATILSAGN).toLowerCase()}
            </ActionMenu.Item>
          )}
          {handlinger.includes(GjennomforingHandling.OPPRETT_TILSAGN_FOR_INVESTERINGER) && (
            <ActionMenu.Item
              onClick={() => {
                navigate(`opprett-tilsagn?type=${TilsagnType.INVESTERING}`);
              }}
            >
              Opprett {avtaletekster.tilsagn.type(TilsagnType.INVESTERING).toLowerCase()}
            </ActionMenu.Item>
          )}
        </Handlinger>
      </KnapperadContainer>
      <TilsagnTable
        emptyStateMessage="Det finnes ingen tilsagn for dette tiltaket"
        data={tilsagn}
      />
    </>
  );
}
