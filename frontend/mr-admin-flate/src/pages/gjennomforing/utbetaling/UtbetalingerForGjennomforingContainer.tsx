import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { Toggles } from "@mr/api-client-v2";
import { Alert, Button, Dropdown } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { utbetalingerByGjennomforingQuery } from "./utbetalingerForGjennomforingLoader";
import { UtbetalingTable } from "@/components/utbetaling/UtbetalingTable";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";

export function UtbetalingerForGjennomforingContainer() {
  const { gjennomforingId } = useParams();
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const navigate = useNavigate();

  const { data: utbetalinger } = useSuspenseQuery({
    ...utbetalingerByGjennomforingQuery(gjennomforingId),
  });

  const { data: enableOkonomi } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI,
    gjennomforing && [gjennomforing.tiltakstype.tiltakskode],
  );

  if (!enableOkonomi) {
    return null;
  }

  return (
    <>
      <KnapperadContainer>
        <HarSkrivetilgang ressurs="Økonomi">
          <Dropdown>
            <Button size="small" variant="secondary" as={Dropdown.Toggle}>
              Handlinger
            </Button>
            <Dropdown.Menu>
              <Dropdown.Menu.GroupedList>
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => {
                    navigate(`skjema`);
                  }}
                >
                  Opprett manuell utbetaling
                </Dropdown.Menu.GroupedList.Item>
              </Dropdown.Menu.GroupedList>
            </Dropdown.Menu>
          </Dropdown>
        </HarSkrivetilgang>
      </KnapperadContainer>
      {utbetalinger.data.length > 0 ? (
        <UtbetalingTable utbetalinger={utbetalinger.data} />
      ) : (
        <Alert style={{ marginTop: "1rem" }} variant="info">
          Det finnes ingen utbetalinger for dette tiltaket
        </Alert>
      )}
    </>
  );
}
