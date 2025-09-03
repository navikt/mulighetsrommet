import { HarTilgang } from "@/components/auth/HarTilgang";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { Avtaletype, Rolle } from "@mr/api-client-v2";
import { Button, Dropdown } from "@navikt/ds-react";
import { useNavigate, useParams } from "react-router";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useTilsagnTableData } from "@/pages/gjennomforing/tilsagn/detaljer/tilsagnDetaljerLoader";
import { TilsagnTable } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnTable";
import { TilsagnType } from "@tiltaksadministrasjon/api-client";

export function TilsagnForGjennomforingPage() {
  const { gjennomforingId } = useParams();
  if (!gjennomforingId) {
    throw Error("Fant ikke gjennomforingId i url");
  }
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);
  const { data: tilsagn } = useTilsagnTableData(gjennomforingId);

  const tilsagnstyper =
    avtale?.avtaletype === Avtaletype.FORHANDSGODKJENT
      ? [TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN, TilsagnType.INVESTERING]
      : [TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN];

  const navigate = useNavigate();

  return (
    <>
      <KnapperadContainer>
        <HarTilgang rolle={Rolle.SAKSBEHANDLER_OKONOMI}>
          <Dropdown>
            <Button size="small" variant="secondary" as={Dropdown.Toggle}>
              Handlinger
            </Button>
            <Dropdown.Menu>
              <Dropdown.Menu.GroupedList>
                {tilsagnstyper.map((type) => (
                  <Dropdown.Menu.GroupedList.Item
                    key={type}
                    onClick={() => {
                      navigate(`opprett-tilsagn?type=${type}`);
                    }}
                  >
                    Opprett {avtaletekster.tilsagn.type(type).toLowerCase()}
                  </Dropdown.Menu.GroupedList.Item>
                ))}
              </Dropdown.Menu.GroupedList>
            </Dropdown.Menu>
          </Dropdown>
        </HarTilgang>
      </KnapperadContainer>
      <TilsagnTable
        emptyStateMessage="Det finnes ingen tilsagn for dette tiltaket"
        data={tilsagn}
      />
    </>
  );
}
