import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { GjennomforingHandling, TilsagnType } from "@tiltaksadministrasjon/api-client";
import { Button, Dropdown } from "@navikt/ds-react";
import { useNavigate } from "react-router";
import { useGjennomforing, useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import { useTilsagnTableData } from "@/pages/gjennomforing/tilsagn/detaljer/tilsagnDetaljerLoader";
import { TilsagnTable } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnTable";
import { useRequiredParams } from "@/hooks/useRequiredParams";

export function TilsagnForGjennomforingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useGjennomforing(gjennomforingId);
  const { data: handlinger } = useGjennomforingHandlinger(gjennomforing.id);
  const { data: tilsagn } = useTilsagnTableData(gjennomforingId);

  const navigate = useNavigate();

  return (
    <>
      <KnapperadContainer>
        <Dropdown>
          <Button size="small" variant="secondary" as={Dropdown.Toggle}>
            Handlinger
          </Button>
          <Dropdown.Menu>
            <Dropdown.Menu.GroupedList>
              {handlinger.includes(GjennomforingHandling.OPPRETT_TILSAGN) && (
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => {
                    navigate(`opprett-tilsagn?type=${TilsagnType.TILSAGN}`);
                  }}
                >
                  Opprett {avtaletekster.tilsagn.type(TilsagnType.TILSAGN).toLowerCase()}
                </Dropdown.Menu.GroupedList.Item>
              )}
              {handlinger.includes(GjennomforingHandling.OPPRETT_EKSTRATILSAGN) && (
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => {
                    navigate(`opprett-tilsagn?type=${TilsagnType.EKSTRATILSAGN}`);
                  }}
                >
                  Opprett {avtaletekster.tilsagn.type(TilsagnType.EKSTRATILSAGN).toLowerCase()}
                </Dropdown.Menu.GroupedList.Item>
              )}
              {handlinger.includes(GjennomforingHandling.OPPRETT_TILSAGN_FOR_INVESTERINGER) && (
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => {
                    navigate(`opprett-tilsagn?type=${TilsagnType.INVESTERING}`);
                  }}
                >
                  Opprett {avtaletekster.tilsagn.type(TilsagnType.INVESTERING).toLowerCase()}
                </Dropdown.Menu.GroupedList.Item>
              )}
            </Dropdown.Menu.GroupedList>
          </Dropdown.Menu>
        </Dropdown>
      </KnapperadContainer>
      <TilsagnTable
        emptyStateMessage="Det finnes ingen tilsagn for dette tiltaket"
        data={tilsagn}
      />
    </>
  );
}
