import {
  Button,
  Checkbox,
  CheckboxGroup,
  ErrorSummary,
  Heading,
  HStack,
  VStack,
} from "@navikt/ds-react";
import {
  ActionFunction,
  Form,
  Link as ReactRouterLink,
  LoaderFunction,
  MetaFunction,
  redirect,
  useActionData,
  useLoaderData,
} from "react-router";
import { ArrangorflateService, ArrangorflateTilsagn, FieldError, Tilskuddstype } from "api-client";
import { destroySession, getSession } from "~/sessions.server";
import { apiHeaders } from "~/auth/auth.server";
import { formaterNOK, jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { useEffect, useRef } from "react";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { tekster } from "~/tekster";
import { FileUpload, FileUploadHandler, parseFormData } from "@mjackson/form-data-parser";
import { FileUploader } from "~/components/fileUploader/FileUploader";
import { errorAt, isValidationError, problemDetailResponse } from "~/utils/validering";
import { yyyyMMddFormatting, formaterPeriode } from "@mr/frontend-common/utils/date";
import { pathByOrgnr } from "~/utils/navigation";
import { Separator } from "~/components/common/Separator";

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 3 av 3: Oppsummering - Opprett krav om utbetaling" },
    {
      name: "description",
      content: "Oppsummering av krav om utbetaling og last opp vedlegg",
    },
  ];
};

type LoaderData = {
  orgnr: string;
  gjennomforingId: string;
  tilsagn: ArrangorflateTilsagn;
  periodeStart: string;
  periodeSlutt: string;
  belop: number;
  kontonummer: string;
  kid?: string;
};

interface ActionData {
  errors?: FieldError[];
}

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { orgnr } = params;
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }

  const session = await getSession(request.headers.get("Cookie"));

  let gjennomforingId: string | undefined;
  let tilsagnId: string | undefined;
  let periodeStart: string | undefined;
  let periodeSlutt: string | undefined;
  let belop: number | undefined;
  let kontonummer: string | undefined;
  let kid: string | undefined;
  if (
    session.get("orgnr") === orgnr &&
    session.get("tilskuddstype") === Tilskuddstype.TILTAK_DRIFTSTILSKUDD
  ) {
    gjennomforingId = session.get("gjennomforingId");
    tilsagnId = session.get("tilsagnId");
    periodeStart = session.get("periodeStart");
    periodeSlutt = session.get("periodeSlutt");
    belop = Number(session.get("belop"));
    kontonummer = session.get("kontonummer");
    kid = session.get("kid");
  }
  if (!gjennomforingId || !tilsagnId || !periodeStart || !periodeSlutt || !belop || !kontonummer)
    throw new Error("Formdata mangler");

  const { data: tilsagn, error } = await ArrangorflateService.getArrangorflateTilsagn({
    path: { id: tilsagnId, orgnr },
    headers: await apiHeaders(request),
  });
  if (error) {
    throw problemDetailResponse(error);
  }

  return {
    orgnr,
    gjennomforingId,
    tilsagn,
    periodeStart,
    periodeSlutt,
    belop,
    kontonummer,
    kid,
  };
};

const uploadHandler: FileUploadHandler = async (fileUpload: FileUpload) => {
  if (fileUpload.fieldName === "vedlegg" && fileUpload.name.endsWith(".pdf")) {
    const bytes = await fileUpload.bytes();
    return new File([bytes], fileUpload.name, { type: fileUpload.type });
  }
};

export const action: ActionFunction = async ({ request }) => {
  const formData = await parseFormData(
    request,
    {
      maxFileSize: 10000000, // 10MB
    },
    uploadHandler,
  );
  const session = await getSession(request.headers.get("Cookie"));
  const errors: FieldError[] = [];

  const vedlegg = formData.getAll("vedlegg") as File[];
  const bekreftelse = formData.get("bekreftelse");

  if (vedlegg.length < 1) {
    errors.push({
      pointer: "/vedlegg",
      detail: "Du må legge ved vedlegg",
    });
  }

  if (!bekreftelse) {
    errors.push({
      pointer: "/bekreftelse",
      detail: "Du må bekrefte at opplysningene er korrekte",
    });
  }

  const orgnr = session.get("orgnr");
  const belop = Number(session.get("belop"));
  const gjennomforingId = session.get("gjennomforingId");
  const tilsagnId = session.get("gjennomforingId");
  const periodeStart = session.get("periodeStart");
  const periodeSlutt = session.get("periodeSlutt");
  const kontonummer = session.get("kontonummer");
  const kid = session.get("kid");

  if (errors.length > 0) {
    return { errors };
  }

  const { error, data: utbetalingId } = await ArrangorflateService.opprettKravOmUtbetaling({
    path: { orgnr: orgnr! },
    body: {
      belop: belop!,
      gjennomforingId: gjennomforingId!,
      tilsagnId: tilsagnId!,
      periodeStart: yyyyMMddFormatting(periodeStart)!,
      periodeSlutt: yyyyMMddFormatting(periodeSlutt)!,
      kontonummer: kontonummer!,
      kidNummer: kid || null,
      tilskuddstype: Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
      vedlegg: vedlegg,
    },
    headers: await apiHeaders(request),
  });

  if (error) {
    if (isValidationError(error)) {
      return { errors: error.errors };
    } else {
      throw problemDetailResponse(error);
    }
  } else {
    return redirect(`${pathByOrgnr(orgnr!).kvittering(utbetalingId)}`, {
      headers: {
        "Set-Cookie": await destroySession(session),
      },
    });
  }
};

export default function OpprettKravOppsummering() {
  const { orgnr, tilsagn, periodeStart, periodeSlutt, belop, kontonummer, kid } =
    useLoaderData<LoaderData>();
  const data = useActionData<ActionData>();
  const errorSummaryRef = useRef<HTMLDivElement>(null);
  const hasError = data?.errors && data.errors.length > 0;

  useEffect(() => {
    if (hasError) {
      errorSummaryRef.current?.focus();
    }
  }, [data, hasError]);

  return (
    <>
      <Heading level="2" spacing size="large">
        Oppsummering
      </Heading>
      <VStack gap="6">
        <Definisjonsliste
          title="Innsendingsinformasjon"
          headingLevel="3"
          definitions={[
            {
              key: "Arrangør",
              value: `${tilsagn.arrangor.navn} - ${orgnr}`,
            },
            { key: "Tiltaksnavn", value: tilsagn.gjennomforing.navn },
          ]}
        />
        <Separator />
        <Definisjonsliste
          title={"Utbetaling"}
          headingLevel="3"
          definitions={[
            {
              key: "Utbetalingsperiode",
              value: formaterPeriode({ start: periodeStart, slutt: periodeSlutt }),
            },
            { key: "Kontonummer", value: kontonummer },
            { key: "KID-nummer", value: kid },
            { key: "Beløp til utbetaling", value: formaterNOK(belop) },
          ]}
        />
        <Separator />
        <Form method="post" encType="multipart/form-data">
          <VStack gap="6">
            <VStack gap="4">
              <Heading level="3" size="medium">
                Vedlegg
              </Heading>
              <FileUploader
                error={errorAt("/vedlegg", data?.errors)}
                maxFiles={10}
                maxSizeMB={3}
                maxSizeBytes={3 * 1024 * 1024}
                id="vedlegg"
              />
            </VStack>
            <Separator />
            <CheckboxGroup error={errorAt("/bekreftelse", data?.errors)} legend={"Bekreftelse"}>
              <Checkbox
                name="bekreftelse"
                value="bekreftet"
                id="bekreftelse"
                error={errorAt("/bekreftelse", data?.errors) !== undefined}
              >
                {tekster.bokmal.utbetaling.oppsummering.bekreftelse}
              </Checkbox>
            </CheckboxGroup>
            {hasError && (
              <ErrorSummary ref={errorSummaryRef}>
                {data.errors?.map((error: FieldError) => {
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
            <HStack gap="4">
              <Button
                as={ReactRouterLink}
                type="button"
                variant="tertiary"
                to={pathByOrgnr(orgnr).opprettKrav.driftstilskudd.utbetaling}
              >
                Tilbake
              </Button>
              <Button type="submit">Bekreft og send inn</Button>
            </HStack>
          </VStack>
        </Form>
      </VStack>
    </>
  );
}
