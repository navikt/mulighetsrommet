import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { Toggles } from "@mr/api-client-v2";
import { Alert, Button, Dropdown } from "@navikt/ds-react";
import { useNavigate, useParams } from "react-router";
import {
  useAdminGjennomforingById,
  useGjennomforingHandlinger,
} from "@/api/gjennomforing/useAdminGjennomforingById";
import { useUtbetalingerByGjennomforing } from "./utbetalingerForGjennomforingLoader";
import { UtbetalingTable } from "@/components/utbetaling/UtbetalingTable";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";

export function UtbetalingerForGjennomforingContainer() {
  const { gjennomforingId } = useParams();
  if (!gjennomforingId) {
    throw Error("Fant ikke gjennomforingId i url");
  }
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const navigate = useNavigate();
  const { data: handlinger } = useGjennomforingHandlinger(gjennomforing.id);

  const { data: utbetalinger } = useUtbetalingerByGjennomforing(gjennomforingId);

  const { data: enableOkonomi } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_UTBETALING,
    [gjennomforing.tiltakstype.tiltakskode],
  );

  if (!enableOkonomi) {
    return null;
  }

  return (
    <>
      <KnapperadContainer>
        <Dropdown>
          <Button size="small" variant="secondary" as={Dropdown.Toggle}>
            Handlinger
          </Button>
          <Dropdown.Menu>
            <Dropdown.Menu.GroupedList>
              {handlinger.opprettKorreksjonPaUtbetaling && (
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => {
                    navigate(`skjema`);
                  }}
                >
                  Opprett korreksjon på utbetaling
                </Dropdown.Menu.GroupedList.Item>
              )}
            </Dropdown.Menu.GroupedList>
          </Dropdown.Menu>
        </Dropdown>
      </KnapperadContainer>
      {utbetalinger.length > 0 ? (
        <UtbetalingTable utbetalinger={utbetalinger} />
      ) : (
        <Alert style={{ marginTop: "1rem" }} variant="info">
          Det finnes ingen utbetalinger for dette tiltaket
        </Alert>
      )}
    </>
  );
}
