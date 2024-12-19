import { Alert, Button, Dropdown } from "@navikt/ds-react";
import { useNavigate } from "react-router-dom";
import { Laster } from "@/components/laster/Laster";
import { InfoContainer } from "@/components/skjema/InfoContainer";
import { useGetTiltaksgjennomforingIdFromUrl } from "@/hooks/useGetTiltaksgjennomforingIdFromUrl";
import { KnapperadContainer } from "@/pages/KnapperadContainer";
import { Toggles } from "@mr/api-client";
import { gjennomforingIsAktiv } from "@mr/frontend-common/utils/utils";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { useTiltaksgjennomforingById } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { TilsagnTabell } from "./TilsagnTabell";
import { useTilsagnForTiltaksgjennomforing } from "@/api/tilsagn/useTilsagnForTiltaksgjennomforing";

export function TilsagnForGjennomforingContainer() {
  const tiltaksgjennomforingId = useGetTiltaksgjennomforingIdFromUrl();
  const { data: tiltaksgjennomforing } = useTiltaksgjennomforingById();
  const { data: tilsagn, isLoading } = useTilsagnForTiltaksgjennomforing(tiltaksgjennomforingId);
  const { data: enableOpprettTilsagn } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_OPPRETT_TILSAGN,
  );
  const navigate = useNavigate();

  if (!enableOpprettTilsagn) {
    return null;
  }

  if (!tiltaksgjennomforing || !tilsagn || isLoading) {
    return <Laster tekst="Laster tilsagn" />;
  }

  return (
    <>
      <InfoContainer>
        <KnapperadContainer>
          <HarSkrivetilgang
            ressurs="Tiltaksgjennomføring"
            condition={gjennomforingIsAktiv(tiltaksgjennomforing.status.status)}
          >
            <Dropdown>
              <Button size="small" as={Dropdown.Toggle}>
                Handlinger
              </Button>
              <Dropdown.Menu>
                <Dropdown.Menu.GroupedList>
                  <Dropdown.Menu.GroupedList.Item
                    onClick={() => {
                      navigate("opprett-tilsagn");
                    }}
                  >
                    Opprett tilsagn
                  </Dropdown.Menu.GroupedList.Item>
                  <Dropdown.Menu.GroupedList.Item
                    onClick={() => {
                      navigate("opprett-tilsagn", {
                        state: {
                          ekstratilsagn: true,
                        },
                      });
                    }}
                  >
                    Opprett ekstratilsagn
                  </Dropdown.Menu.GroupedList.Item>
                </Dropdown.Menu.GroupedList>
              </Dropdown.Menu>
            </Dropdown>
          </HarSkrivetilgang>
        </KnapperadContainer>
        {tilsagn.length > 0 ? (
          <TilsagnTabell tilsagn={tilsagn} />
        ) : (
          <Alert style={{ marginTop: "1rem" }} variant="info">
            Det finnes ingen tilsagn for dette tiltaket
          </Alert>
        )}
      </InfoContainer>
    </>
  );
}
