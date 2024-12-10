import { Alert, Button, Dropdown } from "@navikt/ds-react";
import { Link, useNavigate } from "react-router-dom";
import { useHentTilsagnForTiltaksgjennomforing } from "../../../api/tilsagn/useHentTilsagnForTiltaksgjennomforing";
import { Laster } from "../../../components/laster/Laster";
import { InfoContainer } from "../../../components/skjema/InfoContainer";
import { useGetTiltaksgjennomforingIdFromUrl } from "../../../hooks/useGetTiltaksgjennomforingIdFromUrl";
import { Tilsagnstabell } from "./Tilsagnstabell";
import { KnapperadContainer } from "../../KnapperadContainer";
import { Toggles } from "@mr/api-client";
import { gjennomforingIsAktiv } from "@mr/frontend-common/utils/utils";
import { useFeatureToggle } from "../../../api/features/useFeatureToggle";
import { HarSkrivetilgang } from "../../../components/authActions/HarSkrivetilgang";
import { useTiltaksgjennomforingById } from "../../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";

export function TilsagnForGjennomforingContainer() {
  const tiltaksgjennomforingId = useGetTiltaksgjennomforingIdFromUrl();
  const { data: tiltaksgjennomforing } = useTiltaksgjennomforingById();
  const { data: tilsagn, isLoading } =
    useHentTilsagnForTiltaksgjennomforing(tiltaksgjennomforingId);
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

  if (!tilsagn) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltaksgjennomføring
        <div>
          <Link to="/">Til forside</Link>
        </div>
      </Alert>
    );
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
                  {gjennomforingIsAktiv(tiltaksgjennomforing.status.status) ? (
                    <Dropdown.Menu.GroupedList.Item
                      onClick={() => {
                        navigate("opprett-tilsagn");
                      }}
                    >
                      Opprett tilsagn
                    </Dropdown.Menu.GroupedList.Item>
                  ) : null}
                  {gjennomforingIsAktiv(tiltaksgjennomforing.status.status) ? (
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
                  ) : null}
                </Dropdown.Menu.GroupedList>
              </Dropdown.Menu>
            </Dropdown>
          </HarSkrivetilgang>
        </KnapperadContainer>
        {tilsagn.length > 0 ? (
          <Tilsagnstabell tilsagn={tilsagn} />
        ) : (
          <Alert variant="info">Det finnes ingen tilsagn for dette tiltaket</Alert>
        )}
      </InfoContainer>
    </>
  );
}
