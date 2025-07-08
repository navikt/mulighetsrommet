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
import { ArrangorflateService, ArrangorflateTilsagn, FieldError } from "api-client";
import { destroySession, getSession } from "~/sessions.server";
import { apiHeaders } from "~/auth/auth.server";
import { formaterNOK, jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { useRef } from "react";
import { Definisjonsliste } from "~/components/Definisjonsliste";
import { Separator } from "~/components/Separator";
import { tekster } from "~/tekster";
import { FileUpload, FileUploadHandler, parseFormData } from "@mjackson/form-data-parser";
import { FileUploader } from "~/components/fileUploader/FileUploader";
import { errorAt, isValidationError, problemDetailResponse } from "~/utils/validering";
import { formaterDatoSomYYYYMMDD } from "@mr/frontend-common/utils/date";
import { formaterPeriode } from "~/utils/date";
import { pathByOrgnr } from "~/utils/navigation";

export const meta: MetaFunction = () => {
  return [
    { title: "Opprett krav om utbetaling" },
    {
      name: "description",
      content: "Last opp vedlegg for å opprette et krav om utbetaling",
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

export const loader: LoaderFunction = async ({ request }): Promise<LoaderData> => {
  const session = await getSession(request.headers.get("Cookie"));

  const orgnr = session.get("orgnr");

  const gjennomforingId = session.get("gjennomforingId");
  const tilsagnId = session.get("tilsagnId");
  const periodeStart = session.get("periodeStart");
  const periodeSlutt = session.get("periodeSlutt");
  const belop = Number(session.get("belop"));
  const kontonummer = session.get("kontonummer");
  const kid = session.get("kid");
  if (
    !orgnr ||
    !gjennomforingId ||
    !tilsagnId ||
    !periodeStart ||
    !periodeSlutt ||
    !belop ||
    !kontonummer
  )
    throw new Error("Formdata mangler");

  const { data: tilsagn, error } = await ArrangorflateService.getArrangorflateTilsagn({
    path: { id: tilsagnId },
    headers: await apiHeaders(request),
  });
  if (error || !tilsagn) {
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
      periodeStart: formaterDatoSomYYYYMMDD(periodeStart!),
      periodeSlutt: formaterDatoSomYYYYMMDD(periodeSlutt!),
      kontonummer: kontonummer!,
      kidNummer: kid || null,
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

  return (
    <>
      <Heading level="3" spacing size="large">
        Oppsummering
      </Heading>
      <VStack gap="6">
        <Definisjonsliste
          title="Innsendingsinformasjon"
          headingLevel="4"
          definitions={[
            {
              key: "Arrangør",
              value: `${tilsagn.arrangor.navn} - ${orgnr}`,
            },
            { key: "Tiltaksnavn", value: tilsagn.gjennomforing.navn },
            { key: "Tilsagn", value: tilsagn.bestillingsnummer },
          ]}
        />
        <Separator />
        <Definisjonsliste
          title={"Utbetaling"}
          headingLevel="4"
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
          <VStack gap="4">
            <Heading level="4" size="small">
              Vedlegg
            </Heading>
            <FileUploader
              error={errorAt("/vedlegg", data?.errors)}
              maxFiles={10}
              maxSizeMB={3}
              maxSizeBytes={3 * 1024 * 1024}
              id="vedlegg"
            />
            <Separator />
            <CheckboxGroup error={errorAt("/bekreftelse", data?.errors)} legend={"Bekreftelse"}>
              <Checkbox name="bekreftelse" value="bekreftet" id="bekreftelse">
                {tekster.bokmal.utbetaling.oppsummering.bekreftelse}
              </Checkbox>
            </CheckboxGroup>
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
            <HStack gap="4">
              <Button
                as={ReactRouterLink}
                type="button"
                variant="tertiary"
                to={pathByOrgnr(orgnr).opprettKravUtbetaling}
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
