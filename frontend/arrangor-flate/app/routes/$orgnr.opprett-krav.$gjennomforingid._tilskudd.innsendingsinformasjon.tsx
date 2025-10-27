import {
  Alert,
  BodyShort,
  Button,
  DatePicker,
  ErrorSummary,
  GuidePanel,
  Heading,
  HStack,
  Label,
  Link,
  Select,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import {
  ArrangorflateService,
  FieldError,
  OpprettKravInnsendingsInformasjon,
  OpprettKravInnsendingsInformasjonDatoVelger,
  OpprettKravInnsendingsInformasjonGuidePanelType,
  OpprettKravVeiviserSteg,
  Periode,
  Tilskuddstype,
} from "api-client";
import { SyntheticEvent, useEffect, useMemo, useRef, useState } from "react";
import {
  ActionFunctionArgs,
  Form,
  Link as ReactRouterLink,
  LoaderFunction,
  MetaFunction,
  redirect,
  useActionData,
  useLoaderData,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { errorAt, problemDetailResponse } from "~/utils/validering";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { commitSession, destroySession, getSession } from "~/sessions.server";
import {
  addDuration,
  formaterPeriode,
  isLater,
  isLaterOrSameDay,
  parseDate,
  yyyyMMddFormatting,
} from "@mr/frontend-common/utils/date";
import { getOrgnrGjennomforingIdFrom, pathByOrgnr, pathBySteg } from "~/utils/navigation";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { getStepTitle } from "./$orgnr.opprett-krav.$gjennomforingid._tilskudd";

type LoaderData = {
  orgnr: string;
  gjennomforingId: string;
  innsendingsinformasjon: OpprettKravInnsendingsInformasjon;
  sessionTilsagnId?: string;
  sessionPeriodeStart?: string;
  sessionPeriodeSlutt?: string;
};

export const meta: MetaFunction = ({ matches }) => {
  return [
    {
      title: getStepTitle(matches),
    },
    {
      name: "description",
      content: "Fyll ut grunnleggende innsendingsinformasjon for å opprette krav om utbetaling",
    },
  ];
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { orgnr, gjennomforingId } = getOrgnrGjennomforingIdFrom(params);

  const session = await getSession(request.headers.get("Cookie"));

  let sessionTilsagnId: string | undefined;
  let sessionPeriodeStart: string | undefined;
  let sessionPeriodeSlutt: string | undefined;
  if (
    session.get("orgnr") === orgnr &&
    session.get("tilskuddstype") === Tilskuddstype.TILTAK_DRIFTSTILSKUDD &&
    session.get("gjennomforingId") === gjennomforingId
  ) {
    sessionTilsagnId = session.get("tilsagnId");
    sessionPeriodeStart = session.get("periodeStart");
    sessionPeriodeSlutt = session.get("periodeSlutt");
  }

  const [{ data: innsendingsinformasjon, error: innsendingsinformasjonError }] = await Promise.all([
    ArrangorflateService.getOpprettKravInnsendingsinformasjon({
      path: { orgnr, gjennomforingId },
      headers: await apiHeaders(request),
    }),
  ]);

  if (innsendingsinformasjonError) {
    throw problemDetailResponse(innsendingsinformasjonError);
  }

  return {
    orgnr,
    gjennomforingId,
    innsendingsinformasjon,
    sessionTilsagnId,
    sessionPeriodeStart,
    sessionPeriodeSlutt,
  };
};

export async function action({ request, params }: ActionFunctionArgs) {
  const { orgnr, gjennomforingId } = getOrgnrGjennomforingIdFrom(params);
  const session = await getSession(request.headers.get("Cookie"));
  const errors: FieldError[] = [];

  const formData = await request.formData();
  const intent = formData.get("intent");
  const nesteSteg = formData.get("nesteSteg") as OpprettKravVeiviserSteg;

  if (intent === "cancel") {
    return redirect(pathByOrgnr(orgnr).opprettKrav.tiltaksOversikt, {
      headers: {
        "Set-Cookie": await destroySession(session),
      },
    });
  }

  const periodeStart = formData.get("periodeStart")?.toString();
  const periodeSlutt = formData.get("periodeSlutt")?.toString();
  const periodeInklusiv = formData.get("periodeInklusiv")?.toString();
  const tilsagnId = formData.get("tilsagnId")?.toString();

  if (!periodeStart) {
    errors.push({
      pointer: "/periodeStart",
      detail: "Du må fylle ut fra dato",
    });
  }
  if (!periodeSlutt) {
    errors.push({
      pointer: "/periodeSlutt",
      detail: "Du må fylle ut til dato",
    });
  } else if (isLaterOrSameDay(periodeStart, periodeSlutt)) {
    errors.push({
      pointer: "/periodeSlutt",
      detail: "Periodeslutt må være etter periodestart",
    });
  } else if (!tilsagnId) {
    errors.push({
      pointer: "/tilsagnId",
      detail: "Kan ikke opprette utbetalingskrav uten gyldig tilsagn",
    });
  }

  if (errors.length > 0) {
    return { errors };
  } else {
    session.set("tilskuddstype", Tilskuddstype.TILTAK_DRIFTSTILSKUDD);
    session.set("orgnr", orgnr);
    session.set("gjennomforingId", gjennomforingId);
    session.set("tilsagnId", tilsagnId);
    session.set("periodeInklusiv", periodeInklusiv);
    session.set("periodeStart", yyyyMMddFormatting(periodeStart));
    session.set("periodeSlutt", yyyyMMddFormatting(periodeSlutt));
    return redirect(pathBySteg(nesteSteg, orgnr, gjennomforingId), {
      headers: {
        "Set-Cookie": await commitSession(session),
      },
    });
  }
}

interface ActionData {
  errors: FieldError[];
}

export default function OpprettKravInnsendingsinformasjon() {
  const { orgnr, innsendingsinformasjon, sessionPeriodeStart, sessionPeriodeSlutt } =
    useLoaderData<LoaderData>();
  const data = useActionData<ActionData>();
  const errorSummaryRef = useRef<HTMLDivElement>(null);
  const [valgtPeriode, setValgtPeriode] = useState<Periode | undefined>(() => {
    if (sessionPeriodeStart && sessionPeriodeSlutt) {
      return { start: sessionPeriodeStart, slutt: sessionPeriodeSlutt };
    }
    return undefined;
  });

  const hasError = data?.errors && data.errors.length > 0;

  const relevanteTilsagn = useMemo(() => {
    if (!valgtPeriode) {
      return [];
    }
    return innsendingsinformasjon.tilsagn.filter((tilsagn) => {
      const innenforStart =
        isLaterOrSameDay(valgtPeriode.start, tilsagn.periode.start) &&
        isLater(tilsagn.periode.slutt, valgtPeriode.start);
      const innenforSlutt =
        isLater(valgtPeriode.slutt, tilsagn.periode.start) &&
        isLater(tilsagn.periode.slutt, valgtPeriode.slutt);
      return innenforStart || innenforSlutt;
    });
  }, [innsendingsinformasjon.tilsagn, valgtPeriode]);

  useEffect(() => {
    if (hasError) {
      errorSummaryRef.current?.focus();
    }
  }, [data, hasError]);

  return (
    <>
      <Form method="post">
        <VStack gap="6">
          <Heading level="3" size="large">
            Innsendingsinformasjon
          </Heading>
          <GuidePanelInformation orgnr={orgnr} type={innsendingsinformasjon.guidePanel} />
          <VStack gap="6" className="max-w-2xl">
            <Definisjonsliste definitions={innsendingsinformasjon.definisjonsListe} />
            <VStack gap="1">
              <Label size="small">Periode</Label>
              <BodyShort textColor="subtle" size="small">
                Hvilken periode gjelder kravet for?
              </BodyShort>
              <PeriodeVelgerVarianter
                onPeriodeSelected={setValgtPeriode}
                type={innsendingsinformasjon.datoVelger}
                sessionPeriodeStart={sessionPeriodeStart}
                sessionPeriodeSlutt={sessionPeriodeSlutt}
              />
            </VStack>
            {valgtPeriode && (
              <>
                <Heading level="3" size="small">
                  Tilgjengelige tilsagn
                </Heading>
                <BodyShort size="small" textColor="subtle">
                  Under vises informasjon om antatt forbruk.
                  <br />
                  Hva som blir utbetalt avhenger imidlertid av faktisk forbruk i perioden.
                </BodyShort>
                {relevanteTilsagn.length < 1 ? (
                  <Alert variant="warning">
                    Fant ingen aktive tilsagn for gjennomføringen. Vennligst ta kontakt med Nav.
                  </Alert>
                ) : (
                  <>
                    <input type="hidden" name="tilsagnId" value={relevanteTilsagn[0].id} readOnly />
                    {relevanteTilsagn.map((tilsagn) => (
                      <TilsagnDetaljer key={tilsagn.id} tilsagn={tilsagn} minimal />
                    ))}
                  </>
                )}
              </>
            )}
          </VStack>
          {data?.errors && data.errors.length > 0 && (
            <ErrorSummary ref={errorSummaryRef}>
              {data.errors.map((error: FieldError) => {
                return (
                  <ErrorSummary.Item
                    href={`#${jsonPointerToFieldPath(error.pointer)}`}
                    key={jsonPointerToFieldPath(error.pointer)}
                  >
                    {error.detail}
                  </ErrorSummary.Item>
                );
              })}
            </ErrorSummary>
          )}
          <HStack gap="4" className="mt-4">
            <input
              name="nesteSteg"
              value={innsendingsinformasjon.navigering.neste?.toString()}
              hidden
              readOnly
            />
            <Button type="submit" variant="tertiary" name="intent" value="cancel">
              Avbryt
            </Button>
            <Button type="submit" name="intent" value="submit">
              Neste
            </Button>
          </HStack>
        </VStack>
      </Form>
    </>
  );
}

interface PeriodeVelgerVarianterProps {
  onPeriodeSelected: (periode?: Periode) => void;
  type: OpprettKravInnsendingsInformasjonDatoVelger;
  sessionPeriodeStart?: string;
  sessionPeriodeSlutt?: string;
  errors?: FieldError[];
}

function PeriodeVelgerVarianter({
  onPeriodeSelected,
  type,
  sessionPeriodeStart,
  sessionPeriodeSlutt,
  errors,
}: PeriodeVelgerVarianterProps) {
  switch (type.type) {
    case "DatoVelgerSelect":
      return (
        <PeriodeSelect
          periodeForslag={type.periodeForslag}
          onPeriodeSelected={onPeriodeSelected}
          sessionPeriodeStart={sessionPeriodeStart}
          sessionPeriodeSlutt={sessionPeriodeSlutt}
        />
      );
    case "DatoVelgerRange":
      return (
        <PeriodeVelger
          onPeriodeSelected={onPeriodeSelected}
          sessionPeriodeStart={sessionPeriodeStart}
          sessionPeriodeSlutt={sessionPeriodeSlutt}
          errors={errors}
        />
      );
    case undefined:
      throw Error("Ugyldig DatoVelger variant");
  }
}

interface PeriodeSelectProps {
  onPeriodeSelected: (periode?: Periode) => void;
  periodeForslag: Array<Periode>;
  sessionPeriodeStart?: string;
  sessionPeriodeSlutt?: string;
}

/**
 * Valg av forhåndsdefinerte perioder, sluttdato eksklusiv
 */
function PeriodeSelect({
  onPeriodeSelected,
  periodeForslag,
  sessionPeriodeStart,
  sessionPeriodeSlutt,
}: PeriodeSelectProps) {
  const startRef = useRef<HTMLInputElement>(null);
  const sluttRef = useRef<HTMLInputElement>(null);

  function onChange(e: SyntheticEvent<HTMLSelectElement, Event>) {
    const selectedValue = (e.target as HTMLSelectElement).value;
    if (!selectedValue) {
      startRef.current!.value = "";
      sluttRef.current!.value = "";
      onPeriodeSelected();
      return;
    }
    const selectedPeriode = periodeForslag[Number(selectedValue)];
    startRef.current!.value = selectedPeriode.start;
    sluttRef.current!.value = selectedPeriode.slutt;
    onPeriodeSelected(selectedPeriode);
  }
  return (
    <HStack gap="4">
      <input name="periodeInklusiv" defaultValue="false" readOnly hidden />
      <input ref={startRef} name="periodeStart" defaultValue={sessionPeriodeStart} hidden />
      <input ref={sluttRef} name="periodeSlutt" defaultValue={sessionPeriodeSlutt} hidden />
      <Select
        hideLabel
        label="Hvilken periode gjelder kravet for?"
        size="small"
        name="periode"
        defaultValue={periodeForslag.findIndex(
          (periode) =>
            periode.start === sessionPeriodeStart && periode.slutt === sessionPeriodeSlutt,
        )}
        onChange={onChange}
      >
        <option value="">Velg periode</option>
        {periodeForslag.map((periode, index) => (
          <option key={periode.start} value={index}>
            {formaterPeriode(periode)}
          </option>
        ))}
      </Select>
    </HStack>
  );
}

interface PeriodeVelgerProps {
  onPeriodeSelected: (periode?: Periode) => void;
  sessionPeriodeStart?: string;
  sessionPeriodeSlutt?: string;
  errors?: FieldError[];
}

/**
 * Fritt valg av periode, sluttdato inklusiv
 */
function PeriodeVelger({
  onPeriodeSelected,
  sessionPeriodeStart,
  sessionPeriodeSlutt,
  errors,
}: PeriodeVelgerProps) {
  const {
    datepickerProps: periodeStartPickerProps,
    inputProps: periodeStartInputProps,
    selectedDay: selectedStartDato,
  } = useDatepicker({
    defaultSelected: sessionPeriodeStart ? parseDate(sessionPeriodeStart) : undefined,
  });
  const {
    datepickerProps: periodeSluttPickerProps,
    inputProps: periodeSluttInputProps,
    selectedDay: selectedSluttDato,
  } = useDatepicker({
    defaultSelected: sessionPeriodeSlutt ? parseDate(sessionPeriodeSlutt) : undefined,
  });

  useEffect(() => {
    if (selectedStartDato && selectedSluttDato) {
      return onPeriodeSelected({
        start: yyyyMMddFormatting(selectedStartDato)!,
        // Normaliser til eksklusiv sluttdato, slik som Perioder ellers er
        slutt: yyyyMMddFormatting(addDuration(selectedSluttDato, { days: 1 }))!,
      });
    }
    return onPeriodeSelected();
  }, [selectedStartDato, selectedSluttDato, onPeriodeSelected]);

  return (
    <HStack wrap gap="4">
      <input name="periodeInklusiv" defaultValue="true" readOnly hidden />
      <DatePicker
        {...periodeStartPickerProps}
        showWeekNumber
        dropdownCaption
        id="periodeStartDatepicker"
      >
        <DatePicker.Input
          label="Fra dato"
          size="small"
          error={errorAt("/periodeStart", errors)}
          name="periodeStart"
          id="periodeStart"
          {...periodeStartInputProps}
        />
      </DatePicker>
      <DatePicker
        {...periodeSluttPickerProps}
        showWeekNumber
        dropdownCaption
        id="periodeSluttDatepicker"
      >
        <DatePicker.Input
          label="Til dato"
          size="small"
          error={errorAt("/periodeSlutt", errors)}
          name="periodeSlutt"
          id="periodeSlutt"
          {...periodeSluttInputProps}
        />
      </DatePicker>
    </HStack>
  );
}

interface GuidePanelInformationProps {
  orgnr: string;
  type: OpprettKravInnsendingsInformasjonGuidePanelType | null;
}

function GuidePanelInformation({ orgnr, type }: GuidePanelInformationProps) {
  switch (type) {
    case OpprettKravInnsendingsInformasjonGuidePanelType.INVESTERING_VTA_AFT:
      return (
        <GuidePanel>
          I dette skjemaet kan du sende inn krav som gjelder tilsagn for investeringer. Andre krav
          om utbetaling skal sendes inn via utbetalingene i{" "}
          <Link as={ReactRouterLink} to={pathByOrgnr(orgnr).utbetalinger}>
            Utbetalingsoversikten.
          </Link>
        </GuidePanel>
      );
    case OpprettKravInnsendingsInformasjonGuidePanelType.TIMESPRIS:
      return (
        <GuidePanel>
          I dette skjemaet kan du sende inn fakturakrav for tiltaksgjennomføringer med avtalt
          timespris.
        </GuidePanel>
      );
    case OpprettKravInnsendingsInformasjonGuidePanelType.AVTALT_PRIS:
      return (
        <GuidePanel>
          I dette skjemaet kan du sende inn fakturakrav i henhold til avtalt pris med Nav.
        </GuidePanel>
      );

    case null:
    default:
      return null;
  }
}
