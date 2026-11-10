import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import {
  ArrangorReferanseType,
  TilskuddBehandlingDto,
  TilskuddBehandlingRequest,
  TilskuddMottaker,
  TilskuddMottakerType,
  TilskuddVedtakRequest,
  Valuta,
} from "@tiltaksadministrasjon/api-client";
import { v4 } from "uuid";

export function defaultTilskuddRequest(kostnadssted: string | null = null): TilskuddVedtakRequest {
  return {
    id: v4(),
    tilskuddId: v4(),
    tilskuddOpplaeringType: null,
    belop: null,
    soknadBelop: {
      belop: null,
      valuta: Valuta.NOK,
    },
    kommentarVedtaksbrev: null,
    vedtakResultat: null,
    utbetalingMottaker: null,
    arrangorReferanse: null,
    kidNummer: null,
    fakturanummer: null,
    brukerUtbetalingType: null,
    soknadJournalpostId: null,
    soknadDato: null,
    periodeStart: null,
    periodeSlutt: null,
    kostnadssted,
    kommentarIntern: null,
  };
}

export function defaultTilskuddRequestFromBehandling(
  behandling: TilskuddBehandlingDto,
): TilskuddBehandlingRequest {
  return {
    id: behandling.id,
    gjennomforingId: behandling.gjennomforingId,
    tilskuddVedtak: behandling.tilskuddVedtak.map((t) => ({
      id: t.id,
      tilskuddId: t.tilskuddId,
      tilskuddOpplaeringType: t.tilskuddOpplaeringType,
      soknadBelop: t.soknadBelop,
      periodeStart: yyyyMMddFormatting(t.periode.start) ?? null,
      periodeSlutt: yyyyMMddFormatting(subDuration(t.periode.slutt, { days: 1 })) ?? null,
      vedtakResultat: t.vedtakResultat.type,
      kommentarVedtaksbrev: t.kommentarVedtaksbrev,
      belop: t.utbetalingBelop?.belop ?? null,
      soknadJournalpostId: t.soknadJournalpostId,
      kostnadssted: t.kostnadssted.enhetsnummer,
      soknadDato: t.soknadDato,
      kommentarIntern: t.kommentarIntern,
      ...getMottakerFields(t.utbetalingMottaker),
    })),
  };
}

type TilskuddMottakerFields = Pick<
  TilskuddVedtakRequest,
  | "utbetalingMottaker"
  | "arrangorReferanse"
  | "kidNummer"
  | "fakturanummer"
  | "brukerUtbetalingType"
>;

function getMottakerFields(mottaker: TilskuddMottaker): TilskuddMottakerFields {
  switch (mottaker.type) {
    case "ARRANGOR":
      return {
        utbetalingMottaker: TilskuddMottakerType.ARRANGOR,
        arrangorReferanse:
          mottaker.referanse.type === "KID"
            ? ArrangorReferanseType.KID
            : ArrangorReferanseType.FAKTURANUMMER,
        kidNummer: (mottaker.referanse.type === "KID" && mottaker.referanse.kid) || null,
        fakturanummer:
          (mottaker.referanse.type === "FAKTURANUMMER" && mottaker.referanse.fakturanummer) || null,
        brukerUtbetalingType: null,
      };
    case "BRUKER":
      return {
        utbetalingMottaker: TilskuddMottakerType.BRUKER,
        arrangorReferanse: null,
        kidNummer: null,
        fakturanummer: null,
        brukerUtbetalingType: mottaker.utbetalingType,
      };
  }
}
