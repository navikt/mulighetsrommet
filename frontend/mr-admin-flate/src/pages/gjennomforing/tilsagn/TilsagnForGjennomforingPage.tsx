import { HarTilgang } from "@/components/auth/HarTilgang";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { Avtaletype, Rolle, TilsagnService, TilsagnType } from "@mr/api-client-v2";
import { Alert, Button, Dropdown } from "@navikt/ds-react";
import { useNavigate, useParams } from "react-router";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { TilsagnTable } from "./tabell/TilsagnTable";

function tilsagnForGjennomforingQuery(gjennomforingId?: string) {
  return {
    queryKey: ["tilsagnForGjennomforing", gjennomforingId],
    queryFn: () => TilsagnService.getAll({ query: { gjennomforingId: gjennomforingId! } }),
    enabled: !!gjennomforingId,
  };
}

export function TilsagnForGjennomforingPage() {
  const { gjennomforingId } = useParams();
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);
  const { data: tilsagnForGjennomforing } = useApiSuspenseQuery({
    ...tilsagnForGjennomforingQuery(gjennomforingId),
  });

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
      {tilsagnForGjennomforing.length > 0 ? (
        <TilsagnTable tilsagn={tilsagnForGjennomforing} />
      ) : (
        <Alert style={{ marginTop: "1rem" }} variant="info">
          Det finnes ingen tilsagn for dette tiltaket i Nav Tiltaksadministrasjon
        </Alert>
      )}
    </>
  );
}
