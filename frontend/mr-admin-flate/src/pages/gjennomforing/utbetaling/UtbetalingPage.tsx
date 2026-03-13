import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { BankNoteFillIcon } from "@navikt/aksel-icons";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Outlet } from "react-router";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";

export function UtbetalingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing } = useGjennomforing(gjennomforingId);

  const brodsmuler: Brodsmule[] = [
    { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforing.id}`,
    },
    {
      tittel: "Utbetalinger",
      lenke: `/gjennomforinger/${gjennomforing.id}/utbetalinger`,
    },
    { tittel: "Utbetaling" },
  ];

  return (
    <>
      <title>{utbetalingTekster.title}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner
        heading={utbetalingTekster.header(gjennomforing.navn)}
        ikon={<BankNoteFillIcon color="#2AA758" width="2.5rem" height="2.5rem" />}
      />
      <WhitePaddedBox>
        <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
        <Outlet />
      </WhitePaddedBox>
    </>
  );
}
