import { Link } from "@navikt/ds-react";
import { ArrangorflateUtbetalingStatus } from "@arrangor-utbetalinger/api-client";
import { Link as ReactRouterLink } from "react-router";
import { pathTo } from "~/utils/navigation";

interface UtbetalingTextLinkProps {
  status: ArrangorflateUtbetalingStatus;
  gjennomforingNavn: string;
  utbetalingId: string;
  orgnr: string;
}

export function UtbetalingTextLink({
  status,
  gjennomforingNavn,
  utbetalingId,
  orgnr,
}: UtbetalingTextLinkProps) {
  let linkText: string;
  let linkPath: string;

  switch (status) {
    case ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING:
      linkText = "Start innsending";
      linkPath = pathTo.innsendingsinformasjon(orgnr, utbetalingId);
      break;
    case ArrangorflateUtbetalingStatus.BLOKKERT_FOR_INNSENDING:
      linkText = "Se innsending";
      linkPath = pathTo.innsendingsinformasjon(orgnr, utbetalingId);
      break;
    case ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV:
    case ArrangorflateUtbetalingStatus.UTBETALT:
    case ArrangorflateUtbetalingStatus.AVBRUTT_AV_ARRANGOR:
    case ArrangorflateUtbetalingStatus.AVBRUTT_AV_NAV:
    case ArrangorflateUtbetalingStatus.DELVIS_UTBETALT:
    case ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING:
      linkText = "Se detaljer";
      linkPath = pathTo.detaljer(orgnr, utbetalingId);
      break;
  }

  return (
    <Link
      as={ReactRouterLink}
      aria-label={`${linkText} for krav om utbetaling for ${gjennomforingNavn}`}
      to={linkPath}
      className="whitespace-nowrap"
    >
      {linkText}
    </Link>
  );
}
