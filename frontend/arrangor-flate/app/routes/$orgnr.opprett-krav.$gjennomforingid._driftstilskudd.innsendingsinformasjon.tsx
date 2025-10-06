import {
  Alert,
  BodyLong,
  BodyShort,
  Button,
  DatePicker,
  ErrorSummary,
  GuidePanel,
  Heading,
  HStack,
  Label,
  Link,
  useRangeDatepicker,
  VStack,
} from "@navikt/ds-react";
import {
  ArrangorflateService,
  FieldError,
  OpprettKravInnsendingsInformasjon,
  Tilskuddstype,
} from "api-client";
import { useEffect, useRef } from "react";
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
import { yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { getOrgnrGjennomforingIdFrom, pathByOrgnr } from "~/utils/navigation";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";

type LoaderData = {
  orgnr: string;
  gjennomforingId: string;
  innsendingsinformasjon: OpprettKravInnsendingsInformasjon;
  sessionTilsagnId?: string;
  sessionPeriodeStart?: string;
  sessionPeriodeSlutt?: string;
};

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 1 av 3: Innsendingsinformasjon - Opprett krav om utbetaling" },
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
      query: { tilskudsstype: Tilskuddstype.TILTAK_DRIFTSTILSKUDD },
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

  if (intent === "cancel") {
    return redirect(pathByOrgnr(orgnr).utbetalinger, {
      headers: {
        "Set-Cookie": await destroySession(session),
      },
    });
  }

  const periodeStart = formData.get("periodeStart")?.toString();
  const periodeSlutt = formData.get("periodeSlutt")?.toString();
  const tilsagnId = formData.get("tilsagnId")?.toString();

  if (!tilsagnId) {
    errors.push({
      pointer: "/tilsagnId",
      detail: "Kan ikke opprette utbetalingskrav uten gyldig tilsagn",
    });
  } else if (!periodeStart) {
    errors.push({
      pointer: "/periodeStart",
      detail: "Du må fylle ut fra dato",
    });
  } else if (!periodeSlutt) {
    errors.push({
      pointer: "/periodeSlutt",
      detail: "Du må fylle ut til dato",
    });
  }

  if (errors.length > 0) {
    return { errors };
  } else {
    session.set("tilskuddstype", Tilskuddstype.TILTAK_DRIFTSTILSKUDD);
    session.set("orgnr", orgnr);
    session.set("gjennomforingId", gjennomforingId);
    session.set("tilsagnId", tilsagnId);
    session.set("periodeStart", yyyyMMddFormatting(periodeStart));
    session.set("periodeSlutt", yyyyMMddFormatting(periodeSlutt));
    return redirect(pathByOrgnr(orgnr).opprettKrav.driftstilskuddv2.utbetaling(gjennomforingId), {
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

  const hasError = data?.errors && data.errors.length > 0;

  useEffect(() => {
    if (hasError) {
      errorSummaryRef.current?.focus();
    }
  }, [data, hasError]);

  const {
    datepickerProps,
    fromInputProps: periodeStartInputProps,
    toInputProps: periodeSluttInputProps,
    selectedRange: valgtPeriode,
  } = useRangeDatepicker({
    defaultSelected: {
      from: sessionPeriodeStart ? new Date(sessionPeriodeStart) : undefined,
      to: sessionPeriodeSlutt ? new Date(sessionPeriodeSlutt) : undefined,
    },
  });

  return (
    <>
      <Form method="post">
        <VStack gap="6">
          <Heading level="3" size="large">
            Innsendingsinformasjon
          </Heading>
          <GuidePanel className="mb-2">
            <BodyLong spacing>
              I dette skjemaet kan du sende inn krav som gjelder tilsagn for driftstilskudd.
              <br />
              Andre krav om utbetaling (feks <abbr title="Arbeidsforberedende trening">
                AFT
              </abbr>{" "}
              eller <abbr title="Varig tilrettelagt arbeid">VTA</abbr>) skal sendes inn via
              utbetalingene i{" "}
              <Link as={ReactRouterLink} to={pathByOrgnr(orgnr).utbetalinger}>
                Utbetalingsoversikten.
              </Link>
            </BodyLong>
          </GuidePanel>
          <VStack gap="6" className="max-w-2xl">
            <Definisjonsliste definitions={innsendingsinformasjon.definisjonsListe} />
            <VStack gap="1">
              <Label size="small">Periode</Label>
              <BodyShort textColor="subtle" size="small">
                Hvilken periode gjelder kravet for?
              </BodyShort>
              <HStack gap="4">
                <DatePicker {...datepickerProps} dropdownCaption id="periodeStartDatepicker">
                  <HStack wrap gap="4" justify="center">
                    <DatePicker.Input
                      {...periodeStartInputProps}
                      label="Fra dato"
                      size="small"
                      error={errorAt("/periodeStart", data?.errors)}
                      name="periodeStart"
                      id="periodeStart"
                    />
                    <DatePicker.Input
                      {...periodeSluttInputProps}
                      label="Til dato"
                      size="small"
                      error={errorAt("/periodeSlutt", data?.errors)}
                      name="periodeSlutt"
                      id="periodeSlutt"
                    />
                  </HStack>
                </DatePicker>
              </HStack>
            </VStack>
            {valgtPeriode?.from && valgtPeriode.to && (
              <>
                <Heading level="3" size="small">
                  Tilgjengelige tilsagn
                </Heading>
                {innsendingsinformasjon.tilsagn.length < 1 ? (
                  <Alert variant="warning">
                    Fant ingen aktive tilsagn for gjennomføringen. Vennligst ta kontakt med Nav.
                  </Alert>
                ) : (
                  <>
                    <input
                      type="hidden"
                      name="tilsagnId"
                      value={innsendingsinformasjon.tilsagn[0].id}
                    />
                    {innsendingsinformasjon.tilsagn.map((tilsagn) => (
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
