import { Link } from "@navikt/ds-react";
import { ArrangorflateUtbetalingStatus } from "api-client";
import { Link as ReactRouterLink } from "react-router";
import { pathTo } from "~/utils/navigation";

interface UtbetalingTextLinkProps {
  status: ArrangorflateUtbetalingStatus | undefined;
  gjennomforingNavn: string;
  gjennomforingId: string;
  utbetalingId: string | undefined;
  orgnr: string;
}

export function UtbetalingTextLink({
  status,
  gjennomforingNavn,
  gjennomforingId,
  utbetalingId,
  orgnr,
}: UtbetalingTextLinkProps) {
  if (!utbetalingId) {
    return (
      <Link
        as={ReactRouterLink}
        aria-label={`Start innsending for krav om utbetaling for ${gjennomforingNavn}`}
        to={pathTo.opprettKrav.innsendingsinformasjon(orgnr, gjennomforingId)}
      >
        Start innsending
      </Link>
    );
  }

  switch (status) {
    case ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING:
    case undefined:
      return (
        <Link
          as={ReactRouterLink}
          aria-label={`Start innsending for krav om utbetaling for ${gjennomforingNavn}`}
          to={pathTo.innsendingsinformasjon(orgnr, utbetalingId)}
        >
          Start innsending
        </Link>
      );
    case ArrangorflateUtbetalingStatus.KREVER_ENDRING:
      return (
        <Link
          as={ReactRouterLink}
          aria-label={`Se innsending for krav om utbetaling for ${gjennomforingNavn}`}
          to={pathTo.beregning(orgnr, utbetalingId)}
        >
          Se innsending
        </Link>
      );
    case ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV:
    case ArrangorflateUtbetalingStatus.UTBETALT:
    case ArrangorflateUtbetalingStatus.AVBRUTT:
    case ArrangorflateUtbetalingStatus.DELVIS_UTBETALT:
    case ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING:
      return (
        <Link
          as={ReactRouterLink}
          aria-label={`Se detaljer for krav om utbetaling for ${gjennomforingNavn}`}
          to={pathTo.detaljer(orgnr, utbetalingId)}
        >
          Se detaljer
        </Link>
      );
  }
}
