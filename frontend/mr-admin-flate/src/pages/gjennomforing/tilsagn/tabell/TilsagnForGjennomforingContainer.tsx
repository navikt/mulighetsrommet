import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { KnapperadContainer } from "@/pages/KnapperadContainer";
import { Avtaletype, TilsagnService, TilsagnType } from "@mr/api-client-v2";
import { Alert, Button, Dropdown } from "@navikt/ds-react";
import { useNavigate, useParams } from "react-router";
import { usePotentialAvtale } from "../../../../api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "../../../../api/gjennomforing/useAdminGjennomforingById";
import { TilsagnTabell } from "./TilsagnTabell";
import { useApiSuspenseQuery } from "@mr/frontend-common";

function tilsagnForGjennomforingQuery(gjennomforingId?: string) {
  return {
    queryKey: ["tilsagnForGjennomforing", gjennomforingId],
    queryFn: () => TilsagnService.getAll({ query: { gjennomforingId: gjennomforingId! } }),
    enabled: !!gjennomforingId,
  };
}

export function TilsagnForGjennomforingContainer() {
  const { gjennomforingId } = useParams();
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: avtale } = usePotentialAvtale(gjennomforing?.avtaleId);
  const { data: tilsagnForGjennomforing } = useApiSuspenseQuery({
    ...tilsagnForGjennomforingQuery(gjennomforingId),
  });

  const tilsagnstyper =
    avtale?.avtaletype === Avtaletype.FORHAANDSGODKJENT
      ? [TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN, TilsagnType.INVESTERING]
      : [TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN];

  const navigate = useNavigate();

  return (
    <>
      <KnapperadContainer>
        <HarSkrivetilgang ressurs="Gjennomføring">
          <Dropdown>
            <Button size="small" as={Dropdown.Toggle}>
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
        </HarSkrivetilgang>
      </KnapperadContainer>
      {tilsagnForGjennomforing?.length > 0 ? (
        <TilsagnTabell tilsagn={tilsagnForGjennomforing} />
      ) : (
        <Alert style={{ marginTop: "1rem" }} variant="info">
          Det finnes ingen tilsagn for dette tiltaket i Nav Tiltaksadministrasjon
        </Alert>
      )}
    </>
  );
}
