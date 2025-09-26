import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { FeatureToggle, GjennomforingHandling } from "@tiltaksadministrasjon/api-client";
import { Alert, Button, Dropdown } from "@navikt/ds-react";
import { useNavigate } from "react-router";
import { useGjennomforing, useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import { useUtbetalingerByGjennomforing } from "./utbetalingerForGjennomforingLoader";
import { UtbetalingTable } from "@/components/utbetaling/UtbetalingTable";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { useRequiredParams } from "@/hooks/useRequiredParams";

export function UtbetalingerForGjennomforingContainer() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useGjennomforing(gjennomforingId);
  const { data: handlinger } = useGjennomforingHandlinger(gjennomforingId);
  const { data: utbetalinger } = useUtbetalingerByGjennomforing(gjennomforingId);
  const { data: enableOkonomi } = useFeatureToggle(
    FeatureToggle.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_UTBETALING,
    [gjennomforing.tiltakstype.tiltakskode],
  );

  const navigate = useNavigate();

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
              {handlinger.includes(GjennomforingHandling.OPPRETT_KORREKSJON_PA_UTBETALING) && (
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
        <UtbetalingTable gjennomforingId={gjennomforingId} utbetalinger={utbetalinger} />
      ) : (
        <Alert style={{ marginTop: "1rem" }} variant="info">
          Det finnes ingen utbetalinger for dette tiltaket
        </Alert>
      )}
    </>
  );
}
