import { Alert, Button, Dropdown } from "@navikt/ds-react";
import { useLoaderData, useNavigate } from "react-router-dom";
import { InfoContainer } from "@/components/skjema/InfoContainer";
import { KnapperadContainer } from "@/pages/KnapperadContainer";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { TilsagnTabell } from "./TilsagnTabell";
import { tilsagnForGjennomforingLoader } from "@/pages/tiltaksgjennomforinger/tilsagn/tabell/tilsagnForGjennomforingLoader";

export function TilsagnForGjennomforingContainer() {
  const { tilsagnForGjennomforing } = useLoaderData<typeof tilsagnForGjennomforingLoader>();

  const navigate = useNavigate();

  return (
    <>
      <InfoContainer>
        <KnapperadContainer>
          <HarSkrivetilgang ressurs="Tiltaksgjennomføring">
            <Dropdown>
              <Button size="small" as={Dropdown.Toggle}>
                Handlinger
              </Button>
              <Dropdown.Menu>
                <Dropdown.Menu.GroupedList>
                  <Dropdown.Menu.GroupedList.Item
                    onClick={() => {
                      navigate("opprett-tilsagn?type=TILSAGN");
                    }}
                  >
                    Opprett tilsagn
                  </Dropdown.Menu.GroupedList.Item>
                  <Dropdown.Menu.GroupedList.Item
                    onClick={() => {
                      navigate("opprett-tilsagn?type=EKSTRATILSAGN");
                    }}
                  >
                    Opprett ekstratilsagn
                  </Dropdown.Menu.GroupedList.Item>
                </Dropdown.Menu.GroupedList>
              </Dropdown.Menu>
            </Dropdown>
          </HarSkrivetilgang>
        </KnapperadContainer>
        {tilsagnForGjennomforing.length > 0 ? (
          <TilsagnTabell tilsagn={tilsagnForGjennomforing} />
        ) : (
          <Alert style={{ marginTop: "1rem" }} variant="info">
            Det finnes ingen tilsagn for dette tiltaket
          </Alert>
        )}
      </InfoContainer>
    </>
  );
}
